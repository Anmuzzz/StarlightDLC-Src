package com.isusdlc.systems.modules.modules.other;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.modules.api.ModuleCategory;
import com.isusdlc.systems.modules.api.ModuleInfo;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.utility.neural.GRURotation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@ModuleInfo(
   name = "Rotate Teacher",
   category = ModuleCategory.OTHER,
   desc = "Записывает движения мыши и тренирует GRU нейросеть"
)
public class RotateTeacher extends BaseModule {

   private static final int HIDDEN = 24;
   private static final int INPUT = 2;
   private static final int OUTPUT = 2;
   private static final int SEQ_LEN = 16;
   private static final int EPOCHS = 300;
   private static final double LR = 0.001;
   private static final int BATCH_SIZE = 32;

   private final List<float[]> samples = new ArrayList<>();
   private float prevYaw;
   private float prevPitch;
   private boolean firstTick = true;

   private double[][] Wz, Uz, Wr, Ur, Wh, Uh, Wy;
   private double[] bz, br, bh, by;
   private double[][] dWz, dUz, dWr, dUr, dWh, dUh, dWy;
   private double[] dbz, dbr, dbh, dby;
   private double[][] mWz, vWz, mUz, vUz, mWr, vWr, mUr, vUr;
   private double[][] mWh, vWh, mUh, vUh, mWy, vWy;
   private double[] mbz, vbz, mbr, vbr, mbh, vbh, mby, vby;

   private int adamStep;
   private double meanYaw, meanPitch, stdYaw, stdPitch;

   public RotateTeacher() {
      initWeights();
   }

   @Override
   public void onEnable() {
      samples.clear();
      firstTick = true;
   }

   @Override
   public void onDisable() {
      if (samples.size() < SEQ_LEN + 1) return;

      saveDataset();
      train();
      saveWeights();
      GRURotation.get().reload();
   }

   private final EventListener<ClientPlayerTickEvent> onTick = event -> {
      if (mc.player == null) return;

      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();

      if (firstTick) {
         prevYaw = yaw;
         prevPitch = pitch;
         firstTick = false;
         return;
      }

      float dYaw = wrapDeg(yaw - prevYaw);
      float dPitch = wrapDeg(pitch - prevPitch);
      samples.add(new float[]{dYaw, dPitch});
      prevYaw = yaw;
      prevPitch = pitch;
   };

   private void train() {
      computeNorm();
      double[][] data = normalize();
      int N = data.length - SEQ_LEN;
      double[][][] X = new double[N][SEQ_LEN][INPUT];
      double[][][] Y = new double[N][SEQ_LEN][OUTPUT];

      for (int i = 0; i < N; i++)
         for (int t = 0; t < SEQ_LEN; t++) {
            X[i][t] = data[i + t];
            Y[i][t] = data[i + t + 1];
         }

      for (int epoch = 1; epoch <= EPOCHS; epoch++) {
         Integer[] idx = new Integer[N];
         for (int i = 0; i < N; i++) idx[i] = i;
         shuffleArray(idx);

         double epochLoss = 0;
         int batches = 0;

         for (int b = 0; b < N; b += BATCH_SIZE) {
            int end = Math.min(b + BATCH_SIZE, N);
            zeroGrads();
            double batchLoss = 0;

            for (int i = b; i < end; i++)
               batchLoss += forwardBackward(X[idx[i]], Y[idx[i]], end - b);

            batchLoss /= (end - b);
            epochLoss += batchLoss;
            batches++;
            clipGrads();
            adamStep++;
            adamUpdate();
         }

         if (epoch % 50 == 0) {
            // progress log
         }
      }
   }

   private double forwardBackward(double[][] x, double[][] y, double batchSize) {
      double[][] hs = new double[SEQ_LEN + 1][HIDDEN];
      double[][] zs = new double[SEQ_LEN][HIDDEN];
      double[][] rs = new double[SEQ_LEN][HIDDEN];
      double[][] hcs = new double[SEQ_LEN][HIDDEN];
      double[][] out = new double[SEQ_LEN][OUTPUT];

      for (int t = 0; t < SEQ_LEN; t++) {
         double[] ht = hs[t];
         zs[t] = sigmoid(addB(linear(Wz, x[t], Uz, ht), bz));
         rs[t] = sigmoid(addB(linear(Wr, x[t], Ur, ht), br));
         hcs[t] = tanh(addB(linear(Wh, x[t], Uh, hadamard(rs[t], ht)), bh));
         double[] hn = new double[HIDDEN];
         for (int i = 0; i < HIDDEN; i++)
            hn[i] = (1.0 - zs[t][i]) * ht[i] + zs[t][i] * hcs[t][i];
         hs[t + 1] = hn;
         out[t] = addB(matVec(Wy, hn), by);
      }

      double loss = 0;
      double[][] dOut = new double[SEQ_LEN][OUTPUT];
      for (int t = 0; t < SEQ_LEN; t++)
         for (int i = 0; i < OUTPUT; i++) {
            double err = out[t][i] - y[t][i];
            dOut[t][i] = 2.0 * err / batchSize;
            loss += err * err;
         }
      loss /= batchSize;

      double[] dh_next = new double[HIDDEN];
      for (int t = SEQ_LEN - 1; t >= 0; t--) {
         for (int i = 0; i < OUTPUT; i++) {
            dby[i] += dOut[t][i];
            for (int j = 0; j < HIDDEN; j++)
               dWy[i][j] += dOut[t][i] * hs[t + 1][j];
         }

         double[] dh = matVecT(Wy, dOut[t]);
         for (int i = 0; i < HIDDEN; i++) dh[i] += dh_next[i];

         double[] dhc = new double[HIDDEN];
         double[] dz = new double[HIDDEN];
         double[] dh_prev = new double[HIDDEN];
         for (int i = 0; i < HIDDEN; i++) {
            dhc[i] = dh[i] * zs[t][i];
            dz[i] = dh[i] * (hcs[t][i] - hs[t][i]);
            dh_prev[i] = dh[i] * (1.0 - zs[t][i]);
         }

         double[] dhc_raw = new double[HIDDEN];
         for (int i = 0; i < HIDDEN; i++)
            dhc_raw[i] = dhc[i] * (1.0 - hcs[t][i] * hcs[t][i]);

         for (int i = 0; i < HIDDEN; i++) dbh[i] += dhc_raw[i];

         double[] rh = hadamard(rs[t], hs[t]);
         addOuterGrad(dWh, dhc_raw, x[t]);
         addOuterGrad(dUh, dhc_raw, rh);

         double[] dr = matVecT(Uh, dhc_raw);
         double[] dh_from_Uh = new double[HIDDEN];
         double[] dr_raw = new double[HIDDEN];
         for (int i = 0; i < HIDDEN; i++) {
            dh_from_Uh[i] = dr[i] * rs[t][i];
            dr_raw[i] = dr[i] * hs[t][i] * rs[t][i] * (1.0 - rs[t][i]);
         }

         for (int i = 0; i < HIDDEN; i++) dbr[i] += dr_raw[i];
         addOuterGrad(dWr, dr_raw, x[t]);
         addOuterGrad(dUr, dr_raw, hs[t]);

         double[] dz_raw = new double[HIDDEN];
         for (int i = 0; i < HIDDEN; i++)
            dz_raw[i] = dz[i] * zs[t][i] * (1.0 - zs[t][i]);

         for (int i = 0; i < HIDDEN; i++) dbz[i] += dz_raw[i];
         addOuterGrad(dWz, dz_raw, x[t]);
         addOuterGrad(dUz, dz_raw, hs[t]);

         double[] dh_from_Uz = matVecT(Uz, dz_raw);
         double[] dh_from_Ur = matVecT(Ur, dr_raw);

         for (int i = 0; i < HIDDEN; i++)
            dh_next[i] = dh_prev[i] + dh_from_Uz[i] + dh_from_Ur[i] + dh_from_Uh[i];
      }

      return loss;
   }

   private void adamUpdate() {
      double b1 = 0.9, b2 = 0.999, eps = 1e-8;
      double bc1 = 1.0 - Math.pow(b1, adamStep);
      double bc2 = 1.0 - Math.pow(b2, adamStep);
      adamMat(Wz, dWz, mWz, vWz, bc1, bc2, b1, b2, eps, LR);
      adamMat(Uz, dUz, mUz, vUz, bc1, bc2, b1, b2, eps, LR);
      adamVec(bz, dbz, mbz, vbz, bc1, bc2, b1, b2, eps, LR);
      adamMat(Wr, dWr, mWr, vWr, bc1, bc2, b1, b2, eps, LR);
      adamMat(Ur, dUr, mUr, vUr, bc1, bc2, b1, b2, eps, LR);
      adamVec(br, dbr, mbr, vbr, bc1, bc2, b1, b2, eps, LR);
      adamMat(Wh, dWh, mWh, vWh, bc1, bc2, b1, b2, eps, LR);
      adamMat(Uh, dUh, mUh, vUh, bc1, bc2, b1, b2, eps, LR);
      adamVec(bh, dbh, mbh, vbh, bc1, bc2, b1, b2, eps, LR);
      adamMat(Wy, dWy, mWy, vWy, bc1, bc2, b1, b2, eps, LR);
      adamVec(by, dby, mby, vby, bc1, bc2, b1, b2, eps, LR);
   }

   private void adamMat(double[][] W, double[][] dW, double[][] m, double[][] v,
                        double bc1, double bc2, double b1, double b2, double eps, double lr) {
      for (int i = 0; i < W.length; i++)
         for (int j = 0; j < W[0].length; j++) {
            m[i][j] = b1 * m[i][j] + (1.0 - b1) * dW[i][j];
            v[i][j] = b2 * v[i][j] + (1.0 - b2) * dW[i][j] * dW[i][j];
            W[i][j] -= lr * (m[i][j] / bc1) / (Math.sqrt(v[i][j] / bc2) + eps);
         }
   }

   private void adamVec(double[] w, double[] dw, double[] m, double[] v,
                        double bc1, double bc2, double b1, double b2, double eps, double lr) {
      for (int i = 0; i < w.length; i++) {
         m[i] = b1 * m[i] + (1.0 - b1) * dw[i];
         v[i] = b2 * v[i] + (1.0 - b2) * dw[i] * dw[i];
         w[i] -= lr * (m[i] / bc1) / (Math.sqrt(v[i] / bc2) + eps);
      }
   }

   private void clipGrads() {
      double norm = 0;
      norm += normMat(dWz) + normMat(dUz) + normVec(dbz);
      norm += normMat(dWr) + normMat(dUr) + normVec(dbr);
      norm += normMat(dWh) + normMat(dUh) + normVec(dbh);
      norm += normMat(dWy) + normVec(dby);
      norm = Math.sqrt(norm);
      if (norm > 1.0) {
         double scale = 1.0 / norm;
         scaleMat(dWz, scale); scaleMat(dUz, scale); scaleVec(dbz, scale);
         scaleMat(dWr, scale); scaleMat(dUr, scale); scaleVec(dbr, scale);
         scaleMat(dWh, scale); scaleMat(dUh, scale); scaleVec(dbh, scale);
         scaleMat(dWy, scale); scaleVec(dby, scale);
      }
   }

   private void initWeights() {
      Random rng = new Random(42);
      Wz = xavier(HIDDEN, INPUT, rng); Uz = xavier(HIDDEN, HIDDEN, rng); bz = fill(HIDDEN, -2.0);
      Wr = xavier(HIDDEN, INPUT, rng); Ur = xavier(HIDDEN, HIDDEN, rng); br = fill(HIDDEN, 2.0);
      Wh = xavier(HIDDEN, INPUT, rng); Uh = xavier(HIDDEN, HIDDEN, rng); bh = zeros1(HIDDEN);
      Wy = xavier(OUTPUT, HIDDEN, rng); by = zeros1(OUTPUT);

      dWz = zeros2(HIDDEN, INPUT); dUz = zeros2(HIDDEN, HIDDEN); dbz = zeros1(HIDDEN);
      dWr = zeros2(HIDDEN, INPUT); dUr = zeros2(HIDDEN, HIDDEN); dbr = zeros1(HIDDEN);
      dWh = zeros2(HIDDEN, INPUT); dUh = zeros2(HIDDEN, HIDDEN); dbh = zeros1(HIDDEN);
      dWy = zeros2(OUTPUT, HIDDEN); dby = zeros1(OUTPUT);

      mWz = zeros2(HIDDEN, INPUT); vWz = zeros2(HIDDEN, INPUT);
      mUz = zeros2(HIDDEN, HIDDEN); vUz = zeros2(HIDDEN, HIDDEN);
      mbz = zeros1(HIDDEN); vbz = zeros1(HIDDEN);
      mWr = zeros2(HIDDEN, INPUT); vWr = zeros2(HIDDEN, INPUT);
      mUr = zeros2(HIDDEN, HIDDEN); vUr = zeros2(HIDDEN, HIDDEN);
      mbr = zeros1(HIDDEN); vbr = zeros1(HIDDEN);
      mWh = zeros2(HIDDEN, INPUT); vWh = zeros2(HIDDEN, INPUT);
      mUh = zeros2(HIDDEN, HIDDEN); vUh = zeros2(HIDDEN, HIDDEN);
      mbh = zeros1(HIDDEN); vbh = zeros1(HIDDEN);
      mWy = zeros2(OUTPUT, HIDDEN); vWy = zeros2(OUTPUT, HIDDEN);
      mby = zeros1(OUTPUT); vby = zeros1(OUTPUT);
   }

   private void zeroGrads() {
      zeroMat(dWz); zeroMat(dUz); zeroVec(dbz);
      zeroMat(dWr); zeroMat(dUr); zeroVec(dbr);
      zeroMat(dWh); zeroMat(dUh); zeroVec(dbh);
      zeroMat(dWy); zeroVec(dby);
   }

   private void computeNorm() {
      double sy = 0, sp = 0, sy2 = 0, sp2 = 0;
      int n = samples.size();
      for (float[] s : samples) { sy += s[0]; sp += s[1]; }
      meanYaw = sy / n;
      meanPitch = sp / n;
      for (float[] s : samples) {
         sy2 += (s[0] - meanYaw) * (s[0] - meanYaw);
         sp2 += (s[1] - meanPitch) * (s[1] - meanPitch);
      }
      stdYaw = Math.sqrt(sy2 / n) + 1e-8;
      stdPitch = Math.sqrt(sp2 / n) + 1e-8;
   }

   private double[][] normalize() {
      double[][] d = new double[samples.size()][INPUT];
      for (int i = 0; i < samples.size(); i++) {
         d[i][0] = (samples.get(i)[0] - meanYaw) / stdYaw;
         d[i][1] = (samples.get(i)[1] - meanPitch) / stdPitch;
      }
      return d;
   }

   private void saveWeights() {
      try {
         Files.createDirectories(Path.of("config/isusdlc"));
         JsonObject o = new JsonObject();
         o.add("Wz", matToJson(Wz)); o.add("Uz", matToJson(Uz)); o.add("bz", vecToJson(bz));
         o.add("Wr", matToJson(Wr)); o.add("Ur", matToJson(Ur)); o.add("br", vecToJson(br));
         o.add("Wh", matToJson(Wh)); o.add("Uh", matToJson(Uh)); o.add("bh", vecToJson(bh));
         o.add("Wy", matToJson(Wy)); o.add("by", vecToJson(by));
         o.addProperty("meanYaw", meanYaw);
         o.addProperty("meanPitch", meanPitch);
         o.addProperty("stdYaw", stdYaw);
         o.addProperty("stdPitch", stdPitch);
         Files.writeString(Path.of(GRURotation.WEIGHTS_PATH),
            new GsonBuilder().setPrettyPrinting().create().toJson(o));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void saveDataset() {
      try {
         Files.createDirectories(Path.of("config/isusdlc"));
         JsonArray root = new JsonArray();
         for (float[] s : samples) {
            JsonArray pair = new JsonArray();
            pair.add(s[0]); pair.add(s[1]);
            root.add(pair);
         }
         Files.writeString(Path.of("config/isusdlc/gru_dataset.json"), new Gson().toJson(root));
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   // ---- Math helpers ----

   private double[] linear(double[][] W, double[] x, double[][] U, double[] h) {
      double[] o = matVec(W, x);
      double[] u = matVec(U, h);
      for (int i = 0; i < o.length; i++) o[i] += u[i];
      return o;
   }

   private double[] matVec(double[][] M, double[] v) {
      double[] o = new double[M.length];
      for (int i = 0; i < M.length; i++)
         for (int j = 0; j < v.length; j++)
            o[i] += M[i][j] * v[j];
      return o;
   }

   private double[] matVecT(double[][] M, double[] v) {
      double[] o = new double[M[0].length];
      for (int i = 0; i < M.length; i++)
         for (int j = 0; j < M[0].length; j++)
            o[j] += M[i][j] * v[i];
      return o;
   }

   private void addOuterGrad(double[][] dW, double[] a, double[] b) {
      for (int i = 0; i < a.length; i++)
         for (int j = 0; j < b.length; j++)
            dW[i][j] += a[i] * b[j];
   }

   private double[] addB(double[] a, double[] b) {
      double[] o = new double[a.length];
      for (int i = 0; i < a.length; i++) o[i] = a[i] + b[i];
      return o;
   }

   private double[] hadamard(double[] a, double[] b) {
      double[] o = new double[a.length];
      for (int i = 0; i < a.length; i++) o[i] = a[i] * b[i];
      return o;
   }

   private double[] sigmoid(double[] x) {
      double[] o = new double[x.length];
      for (int i = 0; i < x.length; i++) o[i] = 1.0 / (1.0 + Math.exp(-x[i]));
      return o;
   }

   private double[] tanh(double[] x) {
      double[] o = new double[x.length];
      for (int i = 0; i < x.length; i++) o[i] = Math.tanh(x[i]);
      return o;
   }

   private double[][] xavier(int r, int c, Random rng) {
      double scale = Math.sqrt(2.0 / (r + c));
      double[][] M = new double[r][c];
      for (int i = 0; i < r; i++)
         for (int j = 0; j < c; j++)
            M[i][j] = rng.nextGaussian() * scale;
      return M;
   }

   private double[] zeros1(int n) { return new double[n]; }
   private double[][] zeros2(int r, int c) { return new double[r][c]; }
   private double[] fill(int n, double v) { double[] a = new double[n]; Arrays.fill(a, v); return a; }
   private void zeroMat(double[][] M) { for (double[] r : M) Arrays.fill(r, 0.0); }
   private void zeroVec(double[] v) { Arrays.fill(v, 0.0); }
   private double normMat(double[][] M) { double s = 0; for (double[] r : M) for (double x : r) s += x * x; return s; }
   private double normVec(double[] v) { double s = 0; for (double x : v) s += x * x; return s; }
   private void scaleMat(double[][] M, double s) { for (double[] r : M) for (int j = 0; j < r.length; j++) r[j] *= s; }
   private void scaleVec(double[] v, double s) { for (int i = 0; i < v.length; i++) v[i] *= s; }

   private JsonArray matToJson(double[][] M) {
      JsonArray a = new JsonArray();
      for (double[] r : M) {
         JsonArray row = new JsonArray();
         for (double x : r) row.add(x);
         a.add(row);
      }
      return a;
   }

   private JsonArray vecToJson(double[] v) {
      JsonArray a = new JsonArray();
      for (double x : v) a.add(x);
      return a;
   }

   private float wrapDeg(float d) {
      d %= 360;
      if (d >= 180) d -= 360;
      if (d < -180) d += 360;
      return d;
   }

   private void shuffleArray(Integer[] arr) {
      Random rng = new Random();
      for (int i = arr.length - 1; i > 0; i--) {
         int j = rng.nextInt(i + 1);
         int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
      }
   }
}

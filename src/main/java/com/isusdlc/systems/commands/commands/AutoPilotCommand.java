package com.isusdlc.systems.commands.commands;

import com.isusdlc.elegant;
import com.isusdlc.systems.commands.Command;
import com.isusdlc.systems.commands.CommandBuilder;
import com.isusdlc.systems.commands.CommandContext;
import com.isusdlc.systems.commands.ParameterBuilder;
import com.isusdlc.systems.commands.ValidationResult;
import com.isusdlc.systems.modules.Module;
import com.isusdlc.systems.modules.modules.player.AutoPilotModule;
import com.isusdlc.utility.game.MessageUtility;
import com.isusdlc.utility.interfaces.IMinecraft;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

public class AutoPilotCommand implements IMinecraft {
    public Command command() {
        return CommandBuilder.begin("autopilot")
            .aliases("ap")
            .desc("Автопилот на элитрах")
            .param("action", (ParameterBuilder<String> p) ->
                p.optional().validator(ValidationResult::ok))
            .param("x", (ParameterBuilder<String> p) ->
                p.optional().validator(this::verifyCoord))
            .param("y", (ParameterBuilder<String> p) ->
                p.optional().validator(this::verifyCoord))
            .param("z", (ParameterBuilder<String> p) ->
                p.optional().validator(this::verifyCoord))
            .handler(this::handle)
            .build();
    }

    private ValidationResult verifyCoord(String input) {
        try {
            Integer.parseInt(input);
            return ValidationResult.ok(input);
        } catch (NumberFormatException e) {
            return ValidationResult.error("Координата должна быть числом");
        }
    }

    private void handle(CommandContext ctx) {
        String action = (String) ctx.arguments().get(0);
        String xs;
        String ys;
        String zs;

        Module mod = elegant.getInstance().getModuleManager().getModule(AutoPilotModule.class);

        if (!(mod instanceof AutoPilotModule ap)) {
            MessageUtility.error(Text.of("Модуль AutoPilot не найден"));
            return;
        }

        if ("stop".equalsIgnoreCase(action)) {
            if (ap.isEnabled()) ap.setEnabled(false, false);
            MessageUtility.info(Text.of("§cАвтопилот остановлен"));
            return;
        }

        if ("status".equalsIgnoreCase(action)) {
            Vec3d t = ap.getTarget();
            if (t == null || !ap.isEnabled()) {
                MessageUtility.info(Text.of("§eАвтопилот не активен"));
            } else {
                double dist = mc.player != null ? mc.player.getPos().distanceTo(t) : 0;
                MessageUtility.info(Text.of(
                    String.format("§aЦель: %.0f %.0f %.0f | Дистанция: %.0f", t.getX(), t.getY(), t.getZ(), dist)
                ));
            }
            return;
        }

        // без действия первый токен — это координата X, остальные сдвигаются
        xs = action;
        ys = ctx.arguments().size() > 1 ? (String) ctx.arguments().get(1) : null;
        zs = ctx.arguments().size() > 2 ? (String) ctx.arguments().get(2) : null;

        if (xs != null && ys != null && zs != null) {
            try {
                int x = Integer.parseInt(xs);
                int y = Integer.parseInt(ys);
                int z = Integer.parseInt(zs);
                Vec3d target = new Vec3d(x + 0.5, y, z + 0.5);
                ap.setTarget(target);
                if (!ap.isEnabled()) ap.setEnabled(true, false);
                MessageUtility.info(Text.of(String.format("§aАвтопилот: полёт к %d %d %d", x, y, z)));
            } catch (NumberFormatException e) {
                MessageUtility.error(Text.of("Неверные координаты"));
            }
        } else {
            MessageUtility.error(Text.of("Использование: .autopilot <x> <y> <z> | .autopilot stop | .autopilot status"));
        }
    }
}

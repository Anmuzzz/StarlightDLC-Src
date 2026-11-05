package com.isusdlc.ui.hud;

import java.util.Stack;

public class HudHistoryManager {
   private final Stack<MoveAction> undoStack = new Stack<>();
   private final Stack<MoveAction> redoStack = new Stack<>();

   private static final int MAX_HISTORY = 50;

   public void registerMove(HudElement element, float fromX, float fromY, float toX, float toY) {
      this.undoStack.push(new MoveAction(element, fromX, fromY, toX, toY));
      while (this.undoStack.size() > MAX_HISTORY) {
         this.undoStack.remove(0);
      }
      this.redoStack.clear();
   }

   public void undo() {
      if (!this.undoStack.isEmpty()) {
         MoveAction lastAction = this.undoStack.pop();
         lastAction.element().pos(lastAction.fromX(), lastAction.fromY());
         this.redoStack.push(lastAction);
      }
   }

   public void redo() {
      if (!this.redoStack.isEmpty()) {
         MoveAction redoAction = this.redoStack.pop();
         redoAction.element().pos(redoAction.toX(), redoAction.toY());
         this.undoStack.push(redoAction);
      }
   }

   private record MoveAction(HudElement element, float fromX, float fromY, float toX, float toY) {
   }
}

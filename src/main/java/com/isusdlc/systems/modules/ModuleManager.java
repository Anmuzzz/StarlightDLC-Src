package com.isusdlc.systems.modules;

import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import com.isusdlc.elegant;
import com.isusdlc.systems.event.EventListener;
import com.isusdlc.systems.event.impl.player.ClientPlayerTickEvent;
import com.isusdlc.systems.event.impl.render.HudRenderEvent;
import com.isusdlc.systems.event.impl.window.KeyPressEvent;
import com.isusdlc.systems.event.impl.window.MouseEvent;
import com.isusdlc.systems.modules.exception.UnknownModuleException;
import com.isusdlc.systems.modules.impl.BaseModule;
import com.isusdlc.systems.modules.modules.visuals.ItemRadius;
import com.isusdlc.systems.modules.modules.visuals.JumpCircle;
import com.isusdlc.systems.modules.modules.combat.Aimbot;
import com.isusdlc.systems.modules.modules.combat.AimAssist;
import com.isusdlc.systems.modules.modules.combat.AntiBot;
import com.isusdlc.systems.modules.modules.combat.Criticals;
import com.isusdlc.systems.modules.modules.combat.HitBox;
import com.isusdlc.systems.modules.modules.combat.NoFriendDamage;
import com.isusdlc.systems.modules.modules.combat.TargetStrafe;

import com.isusdlc.systems.modules.modules.combat.ClickPearl;

import com.isusdlc.systems.modules.modules.combat.KillAura;
import com.isusdlc.systems.modules.modules.combat.AutoWeapon;
import com.isusdlc.systems.modules.modules.combat.Reach;
import com.isusdlc.systems.modules.modules.combat.ShiftTap;
import com.isusdlc.systems.modules.modules.combat.TotemTracker;
import com.isusdlc.systems.modules.modules.combat.AutoTotem;
import com.isusdlc.systems.modules.modules.combat.AutoExplosion;

import com.isusdlc.systems.modules.modules.combat.AutoWarden;
import com.isusdlc.systems.modules.modules.combat.TriggerBot;
import com.isusdlc.systems.modules.modules.combat.NoInteract;
import com.isusdlc.systems.modules.modules.player.AutoPotion;
import com.isusdlc.systems.modules.modules.movement.AirStuck;
import com.isusdlc.systems.modules.modules.movement.AutoSprint;

import com.isusdlc.systems.modules.modules.movement.ElytraTarget;
import com.isusdlc.systems.modules.modules.movement.ElytraMotion;
import com.isusdlc.systems.modules.modules.movement.ElytraRecast;

import com.isusdlc.systems.modules.modules.movement.InventoryMove;
import com.isusdlc.systems.modules.modules.movement.NoSlow;
import com.isusdlc.systems.modules.modules.movement.NoWeb;
import com.isusdlc.systems.modules.modules.movement.Speed;
import com.isusdlc.systems.modules.modules.movement.Spider;
import com.isusdlc.systems.modules.modules.movement.Strafe;
import com.isusdlc.systems.modules.modules.movement.SuperFirework;
import com.isusdlc.systems.modules.modules.movement.Timer;
import com.isusdlc.systems.modules.modules.movement.Velocity;
import com.isusdlc.systems.modules.modules.other.Assist;
import com.isusdlc.systems.modules.modules.other.AutoAccept;
import com.isusdlc.systems.modules.modules.other.AutoDuel;
import com.isusdlc.systems.modules.modules.other.AutoJoin;
import com.isusdlc.systems.modules.modules.other.CoordInvite;
import com.isusdlc.systems.modules.modules.other.DeathCords;
import com.isusdlc.systems.modules.modules.other.ItemHighlighter;
import com.isusdlc.systems.modules.modules.other.ItemPickup;
import com.isusdlc.systems.modules.modules.other.NameProtect;
import com.isusdlc.systems.modules.modules.other.NoCommands;
import com.isusdlc.systems.modules.modules.other.ConfigManagerModule;
import com.isusdlc.systems.modules.modules.other.RotateTeacher;
import com.isusdlc.systems.modules.modules.other.Sounds;
import com.isusdlc.systems.modules.modules.other.SpawnBind;
import com.isusdlc.systems.modules.modules.other.SpecBind;
import com.isusdlc.systems.modules.modules.other.YouTubeModule;
import com.isusdlc.systems.modules.modules.other.UnHook;
import com.isusdlc.systems.modules.modules.other.AutoAppleFarm;
import com.isusdlc.systems.modules.modules.other.ClientUsers;
import com.isusdlc.systems.modules.modules.other.LuaModule;
import com.isusdlc.systems.modules.modules.player.AutoEat;
import com.isusdlc.systems.modules.modules.player.AutoInvisible;
import com.isusdlc.systems.modules.modules.player.AutoLeave;
import com.isusdlc.systems.modules.modules.player.AutoRespawn;
import com.isusdlc.systems.modules.modules.player.AutoSwap;
import com.isusdlc.systems.modules.modules.player.AutoTool;
import com.isusdlc.systems.modules.modules.player.AutoPilotModule;
import com.isusdlc.systems.modules.modules.player.ElytraUtils;
import com.isusdlc.systems.modules.modules.player.FreeCamera;
import com.isusdlc.systems.modules.modules.player.Freelook;
import com.isusdlc.systems.modules.modules.player.InvUtils;
import com.isusdlc.systems.modules.modules.player.MiddleClick;

import com.isusdlc.systems.modules.modules.player.MineHelper;
import com.isusdlc.systems.modules.modules.player.NoDelay;
import com.isusdlc.systems.modules.modules.player.NoPush;
import com.isusdlc.systems.modules.modules.player.PlayerUtils;
import com.isusdlc.systems.modules.modules.player.TapeMouse;
import com.isusdlc.systems.modules.modules.player.WindJump;
import com.isusdlc.systems.modules.modules.player.ChestStealer;
import com.isusdlc.systems.modules.modules.visuals.Ambience;
import com.isusdlc.systems.modules.modules.visuals.AspectRatio;
import com.isusdlc.systems.modules.modules.visuals.BlockOverlay;
import com.isusdlc.systems.modules.modules.visuals.CustomFog;
import com.isusdlc.systems.modules.modules.visuals.CustomHitBox;
import com.isusdlc.systems.modules.modules.visuals.HitEffect;
import com.isusdlc.systems.modules.modules.visuals.HitParticles;
import com.isusdlc.systems.modules.modules.visuals.Interface;
import com.isusdlc.systems.modules.modules.visuals.ItemPhysics;
import com.isusdlc.systems.modules.modules.visuals.KillEffects;
import com.isusdlc.systems.modules.modules.visuals.SoulESP;
import com.isusdlc.systems.modules.modules.visuals.MenuModule;
import com.isusdlc.systems.modules.modules.visuals.ObjectInfo;
import com.isusdlc.systems.modules.modules.visuals.PVPAI;
import com.isusdlc.systems.modules.modules.visuals.Prediction;
import com.isusdlc.systems.modules.modules.visuals.Removals;
import com.isusdlc.systems.modules.modules.visuals.SwingAnimation;
import com.isusdlc.systems.modules.modules.visuals.TNTTimer;
import com.isusdlc.systems.modules.modules.visuals.HitFX;
import com.isusdlc.systems.modules.modules.visuals.TargetESP;
import com.isusdlc.systems.modules.modules.visuals.ViewModel;
import com.isusdlc.systems.modules.modules.visuals.World;
import com.isusdlc.systems.modules.modules.visuals.Zoom;
import com.isusdlc.systems.modules.modules.visuals.ChinahatModule;
import com.isusdlc.systems.modules.modules.visuals.ParticleEffects;
import com.isusdlc.systems.modules.modules.visuals.ShulkerPreview;
import com.isusdlc.systems.modules.modules.visuals.SoundESP;
import com.isusdlc.systems.modules.modules.visuals.TrapESP;
import com.isusdlc.systems.modules.modules.visuals.Waypoints;
import com.isusdlc.systems.modules.modules.visuals.Arrows;
import com.isusdlc.systems.modules.modules.visuals.BlockESP;
import com.isusdlc.systems.modules.modules.visuals.CrossHair;
import com.isusdlc.systems.modules.modules.visuals.ESP;
import com.isusdlc.systems.modules.modules.visuals.EnemyInfo;
import com.isusdlc.systems.modules.modules.visuals.FullBright;
import com.isusdlc.systems.modules.modules.visuals.Nametags;
import com.isusdlc.systems.modules.modules.visuals.NoRender;

import com.isusdlc.systems.modules.modules.visuals.cosmetic.CosmeticModule;

import net.minecraft.client.MinecraftClient;
import ru.kotopushka.compiler.sdk.annotations.CompileBytecode;

public class ModuleManager {
   private final List<Module> modules = new ArrayList<>();
   private final java.util.Map<Class<?>, Module> moduleClassCache = new java.util.concurrent.ConcurrentHashMap<>();
   private final java.util.Map<String, Module> moduleNameCache = new java.util.concurrent.ConcurrentHashMap<>();
   private final EventListener<ClientPlayerTickEvent> tickListener;
   private final EventListener<HudRenderEvent> moduleWidgetRenderer;
   private final EventListener<KeyPressEvent> onKeyPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            if (module.getKey() == event.getKey() && module.getKey() != -1 && event.getAction() == 1) {
               module.toggle();
            }
         }
      }
   };
   private final EventListener<MouseEvent> onMouseButtonPress = event -> {
      if (MinecraftClient.getInstance().currentScreen == null) {
         for (Module module : this.getModules()) {
            if (module.getKey() == event.getButton() && module.getKey() != -1 && event.getAction() == 1) {
               module.toggle();
            }
         }
      }
   };

   public ModuleManager(EventListener<ClientPlayerTickEvent> tickListener, EventListener<HudRenderEvent> moduleWidgetRenderer) {
      this.tickListener = tickListener;
      this.moduleWidgetRenderer = moduleWidgetRenderer;
      elegant.getInstance().getEventManager().subscribe(this);
   }

   @CompileBytecode
   public void registerModules() {
      this.register(new TotemTracker());
      this.register(new AutoSprint());
      this.register(new Velocity());
      this.register(new NoSlow());
      this.register(new InventoryMove());
      this.register(new Spider());
      this.register(new Speed());
      this.register(new AirStuck());
      this.register(new NoWeb());
      this.register(new SuperFirework());
      this.register(new Timer());
      this.register(new Strafe());
      this.register(new ElytraMotion());
      this.register(new ElytraRecast());
      this.register(new ElytraTarget());


      this.register(new KillAura());
      this.register(new AutoWeapon());
      this.register(new Aimbot());
      this.register(new AimAssist());
      this.register(new AntiBot());
      this.register(new Criticals());
      this.register(new HitBox());
      this.register(new NoFriendDamage());
      this.register(new TargetStrafe());

      this.register(new ClickPearl());
      this.register(new Reach());
      this.register(new MenuModule());
      this.register(new Removals());
      this.register(new Ambience());
      this.register(new SwingAnimation());
      this.register(new ItemRadius());
      this.register(new TNTTimer());
      this.register(new ViewModel());
      this.register(new Interface());
      this.register(new HitEffect());
      this.register(new TargetESP());
      this.register(new HitFX());
      this.register(new YouTubeModule());
      this.register(new CustomFog());
      this.register(new AspectRatio());
      this.register(new Zoom());
      //this.register(new CustomHitBox());
      this.register(new World());
      this.register(new KillEffects());
      this.register(new Prediction());
      this.register(new ShiftTap());
      this.register(new NoInteract());
      this.register(new WindJump());
      this.register(new ChestStealer());
      this.register(new AutoPotion());
      this.register(new AutoLeave());
      this.register(new AutoRespawn());
      this.register(new AutoTool());
      this.register(new NoPush());

      this.register(new NoDelay());
      this.register(new FreeCamera());
      this.register(new AutoInvisible());
      this.register(new MineHelper());
      this.register(new MiddleClick());
      this.register(new InvUtils());
      this.register(new AutoEat());
      this.register(new PlayerUtils());
      this.register(new ItemPickup());
      this.register(new ItemHighlighter());
      this.register(new ObjectInfo());
      this.register(new NameProtect());
      this.register(new AutoPilotModule());
      this.register(new ElytraUtils());
      this.register(new Freelook());
      this.register(new TapeMouse());
      this.register(new AutoAccept());
      this.register(new DeathCords());
      this.register(new AutoSwap());
      this.register(new AutoJoin());
      this.register(new Assist());
      this.register(new Sounds());
      this.register(new CoordInvite());
      this.register(new AutoDuel());
      this.register(new NoCommands());
      this.register(new UnHook());
      this.register(new SpawnBind());
      this.register(new SpecBind());
      this.register(new ConfigManagerModule());
      this.register(new RotateTeacher());
      this.register(new BlockOverlay());
      this.register(new HitParticles());
      this.register(new ItemPhysics());
      this.register(new AutoTotem());
      this.register(new TriggerBot());
      this.register(new AutoExplosion());

      this.register(new AutoWarden());
      this.register(new ChinahatModule());
      this.register(new ShulkerPreview());
      this.register(new SoundESP());
      this.register(new TrapESP());
      this.register(new Waypoints());
      this.register(new ParticleEffects());
      this.register(new JumpCircle());
      this.register(new SoulESP());
      this.register(new FullBright());
      this.register(new CrossHair());
      this.register(new NoRender());
      this.register(new AutoAppleFarm());
      this.register(new ClientUsers());
      this.register(new LuaModule());
      this.register(new BlockESP());
      this.register(new Arrows());
      this.register(new ESP());
      this.register(new Nametags());
      this.register(new EnemyInfo());

      this.register(CosmeticModule.getInstance());
      //this.register(new PVPAI());
   }

   @CompileBytecode
   public void enableModules() {
      for (Module module : this.modules) {
         if (module.getInfo().enabledByDefault()) {
            module.enable();
         }
      }

      elegant.LOGGER.info("Enabled default modules");
   }

   public void register(BaseModule module) {
      this.modules.add(module);
      this.moduleClassCache.put(module.getClass(), module);
      String name = module.getName();
      // findFirst в старом стриме возвращал первый совпавший — putIfAbsent сохраняет семантику
      this.moduleNameCache.putIfAbsent(name.toLowerCase(), module);
      this.moduleNameCache.putIfAbsent(name.replace(" ", "").toLowerCase(), module);
   }

   public <T extends Module> T getModule(String name) {
      Module module = this.moduleNameCache.get(name.toLowerCase());
      if (module == null) {
         throw new UnknownModuleException(name);
      }

      return (T)module;
   }

   public <T extends Module> T getModule(Class<T> clazz) {
      Module module = this.moduleClassCache.get(clazz);
      if (module == null) {
         throw new UnknownModuleException(clazz.getSimpleName());
      }

      return (T)module;
   }

   public <T extends Module> T getModuleSafe(Class<T> clazz) {
      return (T)this.moduleClassCache.get(clazz);
   }

   public void disableAllModules() {
      for (Module module : this.modules) {
         if (module.isEnabled()) {
            module.disable();
         }
      }
   }

   @Generated
   public List<Module> getModules() {
      return this.modules;
   }

   @Generated
   public EventListener<ClientPlayerTickEvent> getTickListener() {
      return this.tickListener;
   }

   @Generated
   public EventListener<HudRenderEvent> getModuleWidgetRenderer() {
      return this.moduleWidgetRenderer;
   }

   @Generated
   public EventListener<KeyPressEvent> getOnKeyPress() {
      return this.onKeyPress;
   }

   @Generated
   public EventListener<MouseEvent> getOnMouseButtonPress() {
      return this.onMouseButtonPress;
   }
}

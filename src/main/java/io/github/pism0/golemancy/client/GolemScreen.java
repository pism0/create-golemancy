package io.github.pism0.golemancy.client;

import io.github.pism0.golemancy.Golemancy;
import io.github.pism0.golemancy.core.network.GolemNetwork;
import io.github.pism0.golemancy.data.BehaviorConfig;
import io.github.pism0.golemancy.data.GolemDefs;
import io.github.pism0.golemancy.data.GolemSpec;
import io.github.pism0.golemancy.data.InstalledSpell;
import io.github.pism0.golemancy.entity.ConstructEntity;
import io.github.pism0.golemancy.lifecycle.GolemRequests;
import io.github.pism0.golemancy.lifecycle.SpecTooltips;
import io.github.pism0.golemancy.magic.CoreDef;
import io.github.pism0.golemancy.magic.MagicServices;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * The three-tab golem configuration screen (SDD 14.1): Overview, Core, Behaviour.
 *
 * <p>The screen never changes anything itself. Every button sends a {@link GolemNetwork.Request};
 * the server validates it and answers with fresh data, which is what gets displayed.
 */
public final class GolemScreen extends Screen {
    private static final ResourceLocation ATLAS = Golemancy.id("textures/gui/golem_screen.png");
    // Atlas layout: see tools/art/gui.py.
    private static final int PANEL_W = 196;
    private static final int PANEL_H = 166;
    private static final int TAB_W = 28;
    private static final int TAB_H = 24;
    private static final int TAB_V = 170;
    private static final int ICON_U = 60;
    private static final int BAR_FRAME_V = 198;
    private static final int BAR_W = 100;
    private static final int ENERGY_V = 210;
    private static final int HEALTH_V = 218;
    private static final int INTEGRITY_V = 226;

    private static final int TEXT = 0xFF303236;
    private static final int TEXT_DIM = 0xFF5A5E64;
    private static final int TITLE = 0xFFF4F1E6;

    private enum Tab {
        OVERVIEW, CORE, BEHAVIOUR
    }

    private final ConstructEntity golem;
    private GolemNetwork.OpenScreen data;
    private Tab tab = Tab.OVERVIEW;
    private int left;
    private int top;
    private final List<Button> behaviourButtons = new ArrayList<>();

    public GolemScreen(ConstructEntity golem, GolemNetwork.OpenScreen data) {
        super(golem.getDisplayName());
        this.golem = golem;
        this.data = data;
    }

    public int entityId() {
        return golem.getId();
    }

    /** New data from the server after one of our requests. */
    public void update(GolemNetwork.OpenScreen fresh) {
        this.data = fresh;
        rebuildWidgets();
    }

    @Override
    protected void init() {
        left = (width - PANEL_W) / 2;
        top = (height - PANEL_H) / 2 + TAB_H / 2;
        behaviourButtons.clear();
        BehaviorConfig behavior = data.spec().behavior();

        int x = left + 12;
        int w = PANEL_W - 24;
        int y = top + 28;
        behaviourButtons.add(addRenderableWidget(Button.builder(
                Component.translatable("golemancy.screen.profile", Component.translatable("golemancy.profile." + behavior.profile().getSerializedName())),
                b -> send(GolemRequests.Action.SET_PROFILE, (behavior.profile().ordinal() + 1) % BehaviorConfig.Profile.values().length))
                .bounds(x, y, w, 18).build()));
        y += 21;
        behaviourButtons.add(addRenderableWidget(Button.builder(
                Component.translatable("golemancy.screen.move_mode", Component.translatable("golemancy.move_mode." + behavior.moveMode().getSerializedName())),
                b -> send(GolemRequests.Action.SET_MOVE_MODE, (behavior.moveMode().ordinal() + 1) % BehaviorConfig.MoveMode.values().length))
                .bounds(x, y, w / 2 - 2, 18).build()));
        behaviourButtons.add(addRenderableWidget(Button.builder(
                Component.translatable("golemancy.screen.combat", onOff(behavior.combat())),
                b -> send(GolemRequests.Action.SET_COMBAT, behavior.combat() ? 0 : 1))
                .bounds(x + w / 2 + 2, y, w / 2 - 2, 18).build()));
        y += 21;
        behaviourButtons.add(addRenderableWidget(Button.builder(Component.literal("-"),
                b -> send(GolemRequests.Action.SET_RADIUS, Math.max(BehaviorConfig.MIN_RADIUS, behavior.radius() - 2)))
                .bounds(x, y, 18, 18).build()));
        behaviourButtons.add(addRenderableWidget(Button.builder(Component.literal("+"),
                b -> send(GolemRequests.Action.SET_RADIUS, Math.min(BehaviorConfig.MAX_RADIUS, behavior.radius() + 2)))
                .bounds(x + w - 18, y, 18, 18).build()));
        y += 21;
        behaviourButtons.add(addRenderableWidget(Button.builder(
                Component.translatable("golemancy.screen.cast_spells", onOff(behavior.castSpells())),
                b -> send(GolemRequests.Action.SET_CAST_SPELLS, behavior.castSpells() ? 0 : 1))
                .bounds(x, y, w / 2 - 2, 18).build()));
        behaviourButtons.add(addRenderableWidget(Button.builder(Component.translatable("golemancy.screen.set_home"),
                b -> send(GolemRequests.Action.SET_HOME_HERE, 0))
                .bounds(x + w / 2 + 2, y, w / 2 - 2, 18).build()));
        y += 33;
        behaviourButtons.add(addRenderableWidget(Button.builder(Component.translatable("golemancy.screen.pack_up"),
                b -> {
                    send(GolemRequests.Action.PACK_UP, 0);
                    onClose();
                })
                .bounds(x + w / 4, y, w / 2, 18).build()));
        updateButtonVisibility();
    }

    private static Component onOff(boolean value) {
        return value ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF;
    }

    private void send(GolemRequests.Action action, int value) {
        PacketDistributor.sendToServer(new GolemNetwork.Request(golem.getId(), action.ordinal(), value));
    }

    private void updateButtonVisibility() {
        for (Button button : behaviourButtons) {
            button.visible = tab == Tab.BEHAVIOUR;
        }
    }

    @Override
    public void tick() {
        if (!golem.isAlive() || golem.isRemoved() || (minecraft != null && minecraft.player != null
                && minecraft.player.distanceToSqr(golem) > GolemRequests.MAX_DISTANCE * GolemRequests.MAX_DISTANCE * 1.5)) {
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Tab candidate : Tab.values()) {
            int tx = left + 6 + candidate.ordinal() * (TAB_W + 2);
            int ty = top - TAB_H + 3;
            if (mouseX >= tx && mouseX < tx + TAB_W && mouseY >= ty && mouseY < ty + TAB_H) {
                tab = candidate;
                updateButtonVisibility();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Inactive tabs sit behind the panel, the active one in front of it.
        for (Tab candidate : Tab.values()) {
            if (candidate != tab) drawTab(graphics, candidate);
        }
        graphics.blit(ATLAS, left, top, 0, 0, PANEL_W, PANEL_H);
        drawTab(graphics, tab);
    }

    private void drawTab(GuiGraphics graphics, Tab which) {
        boolean active = which == tab;
        int tx = left + 6 + which.ordinal() * (TAB_W + 2);
        int ty = top - TAB_H + 3;
        graphics.blit(ATLAS, tx, ty, active ? 30 : 0, TAB_V, TAB_W, TAB_H);
        graphics.blit(ATLAS, tx + 6, ty + (active ? 4 : 6), ICON_U + which.ordinal() * 18, TAB_V, 16, 16);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        GolemSpec spec = data.spec();
        graphics.drawString(font, golem.getDisplayName(), left + 14, top + 9, TITLE, false);
        Component tabName = Component.translatable("golemancy.screen.tab." + tab.name().toLowerCase(java.util.Locale.ROOT));
        graphics.drawString(font, tabName, left + PANEL_W - 14 - font.width(tabName), top + 9, TITLE, false);

        int x = left + 12;
        int y = top + 29;
        switch (tab) {
            case OVERVIEW -> {
                y = line(graphics, "golemancy.screen.frame", SpecTooltips.frame(spec.frame()), x, y);
                y = line(graphics, "golemancy.screen.material", SpecTooltips.material(spec.material()), x, y);
                y = line(graphics, "golemancy.screen.owner",
                        data.ownerName().isEmpty() ? Component.translatable("golemancy.screen.owner.none") : Component.literal(data.ownerName()), x, y);
                y += 4;
                graphics.drawString(font, Component.translatable("golemancy.screen.health",
                        Math.round(golem.getHealth()), Math.round(golem.getMaxHealth())), x, y, TEXT, false);
                y += 10;
                bar(graphics, x, y, HEALTH_V, golem.getMaxHealth() <= 0 ? 0 : golem.getHealth() / golem.getMaxHealth());
                y += 16;
                graphics.drawString(font, Component.translatable("golemancy.screen.state." + golem.getState().name().toLowerCase(java.util.Locale.ROOT)),
                        x, y, TEXT_DIM, false);
            }
            case CORE -> {
                y = line(graphics, "golemancy.screen.core", SpecTooltips.core(spec.core()), x, y);
                CoreDef core = minecraft == null || minecraft.level == null ? null
                        : GolemDefs.core(minecraft.level.registryAccess(), spec.core()).orElse(null);
                Component school = core == null || core.school().isEmpty()
                        ? Component.translatable("golemancy.screen.school.none")
                        : MagicServices.affinitySource().displayName(core.school().get());
                y = line(graphics, "golemancy.screen.school", school, x, y);
                y += 2;
                // Live value: the synced energy fraction, scaled by the capacity sent with the screen.
                float fraction = golem.getEnergyFraction();
                graphics.drawString(font, Component.translatable("golemancy.screen.energy",
                        Math.round(fraction * data.maxEnergy()), Math.round(data.maxEnergy())), x, y, TEXT, false);
                y += 10;
                bar(graphics, x, y, ENERGY_V, fraction);
                y += 13;
                graphics.drawString(font, Component.translatable("golemancy.screen.drain",
                        String.format(java.util.Locale.ROOT, "%.1f", data.passiveDrain() * 120f)), x, y, TEXT_DIM, false);
                y += 12;
                graphics.drawString(font, Component.translatable("golemancy.screen.integrity",
                        Math.round(spec.coreIntegrity() * 100f)), x, y, TEXT, false);
                y += 10;
                bar(graphics, x, y, INTEGRITY_V, spec.coreIntegrity());
                y += 14;
                if (spec.spells().isEmpty()) {
                    graphics.drawString(font, Component.translatable("golemancy.screen.spells.none"), x, y, TEXT_DIM, false);
                } else {
                    for (InstalledSpell spell : spec.spells()) {
                        graphics.drawString(font, Component.translatable("golemancy.tooltip.spell", SpecTooltips.spell(spell)), x, y, TEXT, false);
                        y += 10;
                    }
                }
            }
            case BEHAVIOUR -> {
                BehaviorConfig behavior = spec.behavior();
                Component radius = Component.translatable("golemancy.screen.radius", behavior.radius());
                graphics.drawString(font, radius, left + (PANEL_W - font.width(radius)) / 2, top + 28 + 42 + 5, TEXT, false);
                Component home = behavior.home()
                        .<Component>map(pos -> Component.translatable("golemancy.screen.home", pos.getX(), pos.getY(), pos.getZ()))
                        .orElse(Component.translatable("golemancy.screen.home.none"));
                graphics.drawString(font, home, x, top + 28 + 84 + 3, TEXT_DIM, false);
                Component hint = Component.translatable("golemancy.profile." + behavior.profile().getSerializedName() + ".desc");
                graphics.drawWordWrap(font, hint, x, top + PANEL_H - 22, PANEL_W - 24, TEXT_DIM);
            }
        }
    }

    private int line(GuiGraphics graphics, String key, Component value, int x, int y) {
        graphics.drawString(font, Component.translatable(key, value), x, y, TEXT, false);
        return y + 12;
    }

    private void bar(GuiGraphics graphics, int x, int y, int fillV, float fraction) {
        graphics.blit(ATLAS, x, y, 0, BAR_FRAME_V, BAR_W + 2, 10);
        int filled = Mth.clamp(Math.round(fraction * BAR_W), 0, BAR_W);
        if (filled > 0) {
            graphics.blit(ATLAS, x + 1, y + 2, 0, fillV, filled, 6);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

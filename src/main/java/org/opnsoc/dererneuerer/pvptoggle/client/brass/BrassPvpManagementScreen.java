package org.opnsoc.dererneuerer.pvptoggle.client.brass;

import gg.essential.elementa.UIComponent;
import gg.essential.elementa.constraints.CenterConstraint;
import gg.essential.elementa.constraints.PixelConstraint;
import gg.essential.universal.UMatrixStack;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.swzo.brass.ui.BrassScreen;
import net.swzo.brass.ui.BrassThemes;
import net.swzo.brass.ui.Colors;
import net.swzo.brass.ui.kit.base.BrassAccent;
import net.swzo.brass.ui.kit.input.BrassSearchField;
import net.swzo.brass.ui.kit.layout.BrassScrollArea;
import net.swzo.brass.ui.kit.media.BrassIcons;
import net.swzo.brass.ui.kit.media.BrassPlayerHead;
import net.swzo.brass.ui.kit.platform.BrassPlatform;
import net.swzo.brass.ui.kit.surface.BrassTooltip;
import net.swzo.brass.ui.kit.surface.BrassWindow;
import net.swzo.brass.ui.kit.text.BrassLabel;
import net.swzo.brass.ui.neoforge.NeoForgePlatform;
import org.opnsoc.dererneuerer.pvptoggle.client.PvpMenuClientBridge;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpMenuActionPayload;
import org.opnsoc.dererneuerer.pvptoggle.network.PvpMenuStatePayload;
import org.opnsoc.dererneuerer.pvptoggle.util.PvpUtil;

import java.awt.Color;
import java.lang.reflect.Constructor;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class BrassPvpManagementScreen extends BrassScreen {
    private static final int FRAME_WIDTH = 286;
    private static final int FRAME_HEIGHT = 190;
    private static final int HEADS_PER_ROW = 8;
    private static final int HEAD_SIZE = 24;
    private static final BrassAccent ALLOWED_ACCENT = BrassThemes.INSTANCE.accentFor(new Color(0x34, 0xD2, 0x7A));

    private PvpMenuStatePayload snapshot;
    private BrassLabel statusLabel;
    private BrassLabel summaryLabel;
    private BrassScrollArea playerScroll;
    private final List<UIComponent> renderedHeads = new ArrayList<>();
    private String query = "";
    private String pendingQuery = "";
    private String previousThemeId;
    private String previousAccentHex;
    private boolean themeApplied;
    private long nextPlayerRefreshAt;

    private BrassPvpManagementScreen(PvpMenuStatePayload snapshot) {
        super(new Color(0, 0, 0, 125));
        this.snapshot = snapshot;
    }

    public static void open(PvpMenuStatePayload snapshot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BrassPvpManagementScreen current) {
            current.updateSnapshot(snapshot);
            return;
        }
        minecraft.setScreen(new BrassPvpManagementScreen(snapshot));
    }

    @Override
    public void afterInitialization() {
        super.afterInitialization();
        previousThemeId = BrassThemes.INSTANCE.getCurrentId();
        previousAccentHex = BrassThemes.INSTANCE.getAccentHex();
        BrassThemes.INSTANCE.apply("grey", "#EF4444");
        themeApplied = true;
        BrassPlatform.Companion.bind(NeoForgePlatform.INSTANCE);

        BrassWindow frame = new BrassWindow(
                "PvP Toggle",
                "Management",
                callback(() -> Minecraft.getInstance().setScreen(null)),
                21,
                false,
                250f,
                155f
        );
        frame.setX(new CenterConstraint());
        frame.setY(new CenterConstraint());
        frame.setWidth(new PixelConstraint((float) FRAME_WIDTH));
        frame.setHeight(new PixelConstraint((float) FRAME_HEIGHT));
        frame.setChildOf(getBackground());

        UIComponent close = iconButton("", BrassIcons.INSTANCE.getNONE(), BrassAccent.Companion.getDANGER(), frame::requestClose);
        close.setX(new PixelConstraint(5f, true));
        close.setY(new PixelConstraint(4f));
        close.setWidth(new PixelConstraint(18f));
        close.setHeight(new PixelConstraint(10f));
        close.setChildOf(frame);
        BrassTooltip.INSTANCE.attach(close, tr("screen.pvptoggle.close"), null, BrassAccent.Companion.getDANGER(), true, null);

        UIComponent body = frame.getContent();
        statusLabel = label("", Colors.INSTANCE.getUI_TEXT(), 1f, body, 8, 7);
        summaryLabel = label("", Colors.INSTANCE.getUI_TEXT_DARK(), 0.9f, body, 8, 19);

        place(iconButton(tr("screen.pvptoggle.enable"), BrassIcons.INSTANCE.getCHECK(), BrassAccent.Companion.getDEFAULT(),
                () -> send(PvpMenuActionPayload.Action.ENABLE, null)), body, 8, 34, 84, 17);
        place(iconButton(tr("screen.pvptoggle.disable"), BrassIcons.INSTANCE.getCLOSE(), BrassAccent.Companion.getDANGER(),
                () -> send(PvpMenuActionPayload.Action.DISABLE, null)), body, 101, 34, 84, 17);
        place(iconButton(tr("screen.pvptoggle.refresh"), BrassIcons.INSTANCE.getRESTORE(), BrassAccent.Companion.getDEFAULT(),
                () -> send(PvpMenuActionPayload.Action.REFRESH, null)), body, 194, 34, 84, 17);

        BrassSearchField search = new BrassSearchField(
                tr("screen.pvptoggle.search"),
                0.12f,
                searchCallback(value -> pendingQuery = value)
        );
        place(search, body, 8, 59, 270, 17);

        playerScroll = new BrassScrollArea(3f, false);
        playerScroll.setX(new PixelConstraint(8f));
        playerScroll.setY(new PixelConstraint(82f));
        playerScroll.setWidth(new PixelConstraint(270f));
        playerScroll.setHeight(new PixelConstraint(76f));
        playerScroll.setChildOf(body);

        refreshLiveText();
        rebuildPlayerHeads();
        nextPlayerRefreshAt = System.currentTimeMillis() + 1000L;
    }

    @Override
    public void onDrawScreen(UMatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (!pendingQuery.equals(query)) {
            query = pendingQuery;
            rebuildPlayerHeads();
        }
        long now = System.currentTimeMillis();
        if (now >= nextPlayerRefreshAt) {
            nextPlayerRefreshAt = now + 1000L;
            send(PvpMenuActionPayload.Action.REFRESH, null);
        }
        refreshLiveText();
        super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onScreenClose() {
        for (UIComponent head : renderedHeads) BrassTooltip.INSTANCE.detach(head);
        super.onScreenClose();
        if (themeApplied) BrassThemes.INSTANCE.apply(previousThemeId, previousAccentHex);
    }

    private void updateSnapshot(PvpMenuStatePayload snapshot) {
        boolean playersChanged = !this.snapshot.players().equals(snapshot.players());
        this.snapshot = snapshot;
        refreshLiveText();
        if (playersChanged) rebuildPlayerHeads();
    }

    private void rebuildPlayerHeads() {
        if (playerScroll == null) return;
        for (UIComponent head : renderedHeads) BrassTooltip.INSTANCE.detach(head);
        renderedHeads.clear();
        playerScroll.clear();

        String filter = query.strip().toLowerCase(Locale.ROOT);
        List<PvpMenuStatePayload.PlayerEntry> players = snapshot.players().stream()
                .filter(player -> filter.isEmpty() || player.name().toLowerCase(Locale.ROOT).contains(filter))
                .toList();

        if (players.isEmpty()) {
            String key = snapshot.players().isEmpty() ? "screen.pvptoggle.no_players" : "screen.pvptoggle.no_search_results";
            label(tr(key), Colors.INSTANCE.getUI_TEXT_DARK(), 0.9f, playerScroll.getContent(), 3, 4);
            return;
        }

        for (int index = 0; index < players.size(); index++) {
            PvpMenuStatePayload.PlayerEntry player = players.get(index);
            PvpMenuActionPayload.Action action = actionFor(player);
            BrassAccent accent = accentFor(player);
            BrassPlayerHead head = new BrassPlayerHead(
                    player.name(),
                    (float) HEAD_SIZE,
                    BrassPlayerHead.Source.GAME,
                    false,
                    null
            );
            head.setAccent(accent);
            head.setClickable(true);
            head.setX(new PixelConstraint((float) (3 + index % HEADS_PER_ROW * 31)));
            head.setY(new PixelConstraint((float) (3 + index / HEADS_PER_ROW * 31)));
            head.setChildOf(playerScroll.getContent());
            head.onMouseClickConsumer(event -> {
                if (event.getMouseButton() == 0) send(action, player.id());
            });
            BrassTooltip.INSTANCE.attachLazy(
                    head,
                    textSupplier(player::name),
                    textSupplier(() -> playerTooltip(player)),
                    accent,
                    true,
                    null
            );
            renderedHeads.add(head);
        }
    }

    private void refreshLiveText() {
        if (statusLabel == null || summaryLabel == null) return;

        long now = System.currentTimeMillis();
        String status;
        Color color;
        if (snapshot.pvpOff()) {
            status = tr("screen.pvptoggle.status_off");
            color = Colors.INSTANCE.getDANGER();
        } else if (snapshot.pendingUntil() > now) {
            status = tr("screen.pvptoggle.status_pending") + " · " + PvpUtil.formatRemaining(snapshot.pendingUntil());
            color = Colors.INSTANCE.getDANGER();
        } else {
            status = tr("screen.pvptoggle.status_on");
            color = Colors.INSTANCE.getUI_TEXT();
        }

        statusLabel.setText(tr("screen.pvptoggle.status") + ": " + status);
        statusLabel.setTint(color);
        summaryLabel.setText(tr(
                "screen.pvptoggle.summary",
                snapshot.blockedCount(),
                snapshot.pendingBlockedCount(),
                snapshot.configuredDelayMinutes()
        ));
    }

    private String playerTooltip(PvpMenuStatePayload.PlayerEntry player) {
        String relation;
        if (player.relation() == PvpMenuStatePayload.Relation.BLOCKED) {
            relation = tr("screen.pvptoggle.blocked");
        } else if (player.relation() == PvpMenuStatePayload.Relation.PENDING) {
            relation = tr("screen.pvptoggle.pending") + " · " + PvpUtil.formatRemaining(player.pendingUntil());
        } else {
            relation = player.canAttack() ? tr("screen.pvptoggle.allowed") : tr("screen.pvptoggle.protected");
        }
        return relation + " · " + actionLabel(player);
    }

    private static PvpMenuActionPayload.Action actionFor(PvpMenuStatePayload.PlayerEntry player) {
        return player.relation() == PvpMenuStatePayload.Relation.ALLOWED
                ? PvpMenuActionPayload.Action.BLOCK
                : PvpMenuActionPayload.Action.UNBLOCK;
    }

    private static BrassAccent accentFor(PvpMenuStatePayload.PlayerEntry player) {
        if (player.relation() != PvpMenuStatePayload.Relation.ALLOWED) return BrassAccent.Companion.getDANGER();
        return player.canAttack() ? ALLOWED_ACCENT : BrassAccent.Companion.getDEFAULT();
    }

    private static String actionLabel(PvpMenuStatePayload.PlayerEntry player) {
        if (player.relation() == PvpMenuStatePayload.Relation.BLOCKED) return tr("screen.pvptoggle.unblock");
        if (player.relation() == PvpMenuStatePayload.Relation.PENDING) return tr("screen.pvptoggle.cancel");
        return tr("screen.pvptoggle.block");
    }

    private void send(PvpMenuActionPayload.Action action, UUID target) {
        PvpMenuClientBridge.sendAction(action, target);
    }

    private static BrassLabel label(String text, Color color, float scale, UIComponent parent, int x, int y) {
        BrassLabel label = new BrassLabel(text, color, true, scale);
        label.setX(new PixelConstraint((float) x));
        label.setY(new PixelConstraint((float) y));
        label.setChildOf(parent);
        return label;
    }

    private static UIComponent iconButton(String label, BrassIcons.Icon icon, BrassAccent accent, Runnable action) {
        try {
            Class<?> callbackType = Class.forName("kotlin.jvm.functions.Function0");
            Object callback = Proxy.newProxyInstance(
                    callbackType.getClassLoader(),
                    new Class<?>[]{callbackType},
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("invoke")) {
                            action.run();
                            return null;
                        }
                        if (method.getName().equals("toString")) return label;
                        if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                        if (method.getName().equals("equals")) return proxy == arguments[0];
                        return null;
                    }
            );
            Class<?> buttonType = Class.forName("net.swzo.brass.ui.kit.input.BrassIconButton");
            Constructor<?> constructor = buttonType.getConstructor(String.class, BrassIcons.Icon.class, BrassAccent.class, callbackType);
            return (UIComponent) constructor.newInstance(label, icon, accent, callback);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create BrassUI button", exception);
        }
    }

    private static void place(UIComponent component, UIComponent parent, int x, int y, int width, int height) {
        component.setX(new PixelConstraint((float) x));
        component.setY(new PixelConstraint((float) y));
        component.setWidth(new PixelConstraint((float) width));
        component.setHeight(new PixelConstraint((float) height));
        component.setChildOf(parent);
    }

    @SuppressWarnings("rawtypes")
    private static Function0 callback(Runnable action) {
        return () -> {
            action.run();
            return null;
        };
    }

    @SuppressWarnings("rawtypes")
    private static Function1 searchCallback(Consumer<String> consumer) {
        return value -> {
            consumer.accept((String) value);
            return null;
        };
    }

    @SuppressWarnings("rawtypes")
    private static Function0 textSupplier(Supplier<String> supplier) {
        return supplier::get;
    }

    private static String tr(String key, Object... arguments) {
        return Component.translatable(key, arguments).getString();
    }
}

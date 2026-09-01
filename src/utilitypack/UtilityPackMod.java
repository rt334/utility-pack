package utilitypack;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.TextField;
import arc.util.Log;
import arc.util.Time;
import mindustry.core.GameState;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.input.Binding;
import mindustry.mod.Mod;
import mindustry.mod.Mods;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SettingsMenuDialog;

import static mindustry.Vars.*;

/**
 * 更多实用功能：方块搜索 + 多人暂停。
 * - 方块搜索：在方块选择菜单添加搜索栏，可跨全部分类搜索所有合法方块（含拼音）。
 * - 多人暂停：多人游戏中允许其他玩家请求暂停/解除暂停，支持管理员/自定义白名单模式。
 */
public class UtilityPackMod extends Mod {
    public static Mods.LoadedMod MOD;

    /**
     * 自定义设置项：在设置表中插入任意内容（分隔线、按钮等）。
     * 通过 SettingsTable.pref() 注册进设置列表，rebuild（恢复默认/切换分类）时自动保留；
     * name 传 null，恢复默认设置时不会被删除。
     */
    public static class CustomSetting extends SettingsMenuDialog.SettingsTable.Setting {
        private final Cons<SettingsMenuDialog.SettingsTable> cons;

        public CustomSetting(Cons<SettingsMenuDialog.SettingsTable> cons) {
            super(null);
            this.cons = cons;
        }

        @Override
        public void add(SettingsMenuDialog.SettingsTable table) {
            cons.get(table);
            table.row();
        }
    }

    public UtilityPackMod() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            MOD = mods.getMod(UtilityPackMod.class);
            if (MOD != null) MOD.meta.subtitle = MOD.meta.version;
        });
    }

    /** 检测是否已启用 Silicon 模组（仅检测已启用状态；游戏内禁用（setEnabled=false）不算启用，文件仍在 mods 目录） */
    private static boolean hasSilicon() {
        for (Mods.LoadedMod mod : mods.list()) {
            if ("silicon".equals(mod.meta.name) && mod.enabled()) return true;
        }
        return false;
    }

    /** 检测到 Silicon 时弹出提示，建议禁用 */
    private static void showConflictDialog() {
        BaseDialog dialog = new BaseDialog(Core.bundle.get("conflict.silicon.title"));
        dialog.cont.add(Core.bundle.get("conflict.silicon.body")).width(420f).pad(12f);
        dialog.buttons.button(Core.bundle.get("conflict.silicon.ok"), Styles.defaultt, dialog::hide).size(140f, 44f);
        dialog.show();
    }

    @Override
    public void init() {
        // —— 冲突检测：若已启用 Silicon 模组，提示禁用（方块搜索/多人暂停重复注册会冲突）——
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (hasSilicon()) {
                Log.info("Detected Silicon mod enabled - showing conflict warning.");
                showConflictDialog();
            }
        });

        // —— 方块搜索 ——
        utilitypack.ui.BlockSearch.init();

        // —— 主界面自动检查 GitHub 更新（可在设置中关闭；有更新才显示横幅）——
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (Core.settings.getBool("updatecheck.autoCheck", true)) {
                UpdateChecker.check();
            }
            UpdateChecker.setupBanner();
        });

        // —— 设置分类：方块搜索 + 多人暂停 ——
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (ui == null) return;
            Mods.LoadedMod mod = mods.getMod(UtilityPackMod.class);
            Drawable icon = (mod != null && mod.iconTexture != null)
                    ? new TextureRegionDrawable(new TextureRegion(mod.iconTexture)) : Styles.black6;
            ui.settings.addCategory("@settings.utilitypack.meta.category.name", icon, st -> {
                // —— 方块搜索设置 ——
                st.checkPref("blocksearch.showHistory", true);
                st.checkPref("blocksearch.clearOnSelect", true);
                // 灰色细线：方块搜索设置与多人暂停设置分隔（注册为设置项，rebuild 时保留）
                st.pref(new CustomSetting(t -> t.image(Tex.whiteui).growX().height(2f).color(Pal.gray).padTop(8f).padBottom(8f)));
                // —— 多人暂停设置 ——
                st.sliderPref("pauseMode", 0, 0, 2, 1,
                        i -> Core.bundle.get("setting.pauseMode.value." + i, String.valueOf(i)),
                        i -> {
                            Vars.pauseMode = i;
                            if (net.client()) Call.serverPacketReliable("pause-setmode", String.valueOf(i));
                        });
                st.checkPref("pauseRequest", true);
                st.pref(new CustomSetting(t -> t.button(Core.bundle.get("setting.pauseWhitelist.name"), Styles.defaultt,
                        UtilityPackMod::showWhitelistDialog).width(200f).padTop(6f)));
                // 灰色细线：多人暂停设置与更新设置分隔（注册为设置项，rebuild 时保留）
                st.pref(new CustomSetting(t -> t.image(Tex.whiteui).growX().height(2f).color(Pal.gray).padTop(8f).padBottom(8f)));
                // —— 更新设置 ——
                st.checkPref("updatecheck.autoCheck", true);
                st.pref(new CustomSetting(t -> t.button(Core.bundle.get("setting.checkUpdate.name"), Styles.defaultt,
                        () -> UpdateChecker.check(true)).width(200f).padTop(6f)));
                // 灰色细线：与「恢复默认设置」分隔（注册为设置项，rebuild 时保留）
                st.pref(new CustomSetting(t -> t.image(Tex.whiteui).growX().height(2f).color(Pal.gray).padTop(8f).padBottom(8f)));
            });
        });

        // —— 多人暂停：网络包处理 ——
        Events.on(EventType.ClientLoadEvent.class, e -> {
            if (netServer != null) {
                netServer.addPacketHandler("pause", (p, time) -> {
                    if (p.admin || p.name.equals(state.map.author())) {
                        state.set(state.isPaused() ? GameState.State.playing : GameState.State.paused);
                        Call.clientPacketReliable(p.con, "paused", time);
                        return;
                    }

                    if (Vars.pauseMode == 0) return;

                    if (Vars.pauseMode == 1) {
                        state.set(state.isPaused() ? GameState.State.playing : GameState.State.paused);
                        Call.clientPacketReliable(p.con, "paused", time);
                        return;
                    }

                    if (Vars.pauseMode == 2 && Vars.pauseWhitelist.contains(p.name)) {
                        state.set(state.isPaused() ? GameState.State.playing : GameState.State.paused);
                        Call.clientPacketReliable(p.con, "paused", time);
                    }
                });

                netServer.addPacketHandler("pause-setmode", (p, data) -> {
                    if (!p.admin && !p.name.equals(state.map.author())) return;
                    try {
                        Vars.pauseMode = Integer.parseInt(data.trim());
                        if (Vars.pauseMode < 0 || Vars.pauseMode > 2) Vars.pauseMode = 0;
                    } catch (NumberFormatException ignored) {}
                });

                netServer.addPacketHandler("pause-grant", (p, data) -> {
                    if (!p.admin && !p.name.equals(state.map.author())) return;
                    String target = data.trim();
                    if (target.isEmpty()) return;
                    if (!Vars.pauseWhitelist.contains(target)) {
                        Vars.pauseWhitelist.add(target);
                    }
                });

                netServer.addPacketHandler("pause-revoke", (p, data) -> {
                    if (!p.admin && !p.name.equals(state.map.author())) return;
                    String target = data.trim();
                    Vars.pauseWhitelist.remove(target);
                });
            }

            netClient.addPacketHandler("paused", (s) -> {
                Vars.pause.complete = true;
            });
        });

        // —— 多人暂停：客户端暂停键请求 ——
        Events.run(EventType.Trigger.update, () -> {
            if (!state.isGame()) return;
            if (net.client() && Core.settings.getBool("pauseRequest", true)) {
                if (Core.input.keyTap(Binding.pause)) {
                    String time = String.valueOf((long) Time.time);
                    Call.serverPacketReliable("pause", time);
                    Vars.pause = new Vars.Pause(time);
                } else if (!Vars.pause.complete && Time.time - Float.parseFloat(Vars.pause.time) > 60f) {
                    String time = String.valueOf((long) Time.time);
                    Call.serverPacketReliable("pause", time);
                    Vars.pause = new Vars.Pause(time);
                }
            }
        });

        // —— 多人暂停：聊天命令 !pause ——
        Events.on(EventType.PlayerChatEvent.class, e -> {
            String msg = e.message;
            if (msg == null || !msg.startsWith("!pause")) return;
            handlePauseCommand(e.player, msg);
        });
    }

    public static void showWhitelistDialog() {
        BaseDialog dialog = new BaseDialog(Core.bundle.get("hubWhitelist.title"));
        dialog.cont.top();

        final Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            dialog.cont.clearChildren();
            dialog.cont.top();

            if (Vars.pauseWhitelist.isEmpty()) {
                dialog.cont.add(Core.bundle.get("hubWhitelist.empty")).color(Color.lightGray).pad(16f);
            } else {
                for (int i = 0; i < Vars.pauseWhitelist.size; i++) {
                    String name = Vars.pauseWhitelist.get(i);
                    dialog.cont.row();
                    dialog.cont.table(t -> {
                        t.add(name).growX().left();
                        t.button(Core.bundle.get("hubWhitelist.remove"), Styles.flatBordert, () -> {
                            Vars.pauseWhitelist.remove(name);
                            if (net.client()) Call.serverPacketReliable("pause-revoke", name);
                            rebuild[0].run();
                        }).padLeft(8f);
                    }).fillX().pad(4f).padLeft(8f).padRight(8f);
                }
            }

            dialog.cont.row();
            dialog.cont.table(t -> {
                TextField field = t.field("", text -> {}).growX().pad(8f).get();
                field.setMessageText(Core.bundle.get("hubWhitelist.placeholder"));
                t.button(Core.bundle.get("hubWhitelist.add"), Styles.flatBordert, () -> {
                    String input = field.getText().trim();
                    if (!input.isEmpty() && !Vars.pauseWhitelist.contains(input)) {
                        Vars.pauseWhitelist.add(input);
                        if (net.client()) Call.serverPacketReliable("pause-grant", input);
                        field.clearText();
                        rebuild[0].run();
                    }
                }).padLeft(8f);
            }).fillX().pad(8f);
        };

        rebuild[0].run();
        dialog.closeOnBack();
        dialog.show();
    }

    private void handlePauseCommand(Player p, String msg) {
        String[] parts = msg.split(" ");
        if (parts.length < 2) return;

        boolean isHost = p.admin || p.name.equals(state.map.author());

        switch (parts[1]) {
            case "on":
                if (!isHost) return;
                Vars.pauseMode = 1;
                Call.infoMessage(p.con, "[accent]Pause mode: Admins only");
                break;
            case "off":
                if (!isHost) return;
                Vars.pauseMode = 0;
                Call.infoMessage(p.con, "[accent]Pause mode: Off");
                break;
            case "custom":
                if (!isHost) return;
                Vars.pauseMode = 2;
                Call.infoMessage(p.con, "[accent]Pause mode: Custom whitelist");
                break;
            case "grant":
                if (!isHost || parts.length < 3) return;
                String grantTarget = parts[2];
                if (!Vars.pauseWhitelist.contains(grantTarget)) {
                    Vars.pauseWhitelist.add(grantTarget);
                }
                Call.infoMessage(p.con, "[accent]Granted pause to: " + grantTarget);
                break;
            case "revoke":
                if (!isHost || parts.length < 3) return;
                String revokeTarget = parts[2];
                Vars.pauseWhitelist.remove(revokeTarget);
                Call.infoMessage(p.con, "[accent]Revoked pause from: " + revokeTarget);
                break;
            case "list":
                if (!isHost) return;
                String list = Vars.pauseWhitelist.isEmpty() ? "(empty)" : Vars.pauseWhitelist.toString(", ");
                Call.infoMessage(p.con, "[accent]Whitelist: " + list);
                break;
        }
    }
}

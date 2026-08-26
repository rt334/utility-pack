package utilitypack;

import arc.struct.Seq;

/**
 * 更多实用功能的自定义 Vars：多人暂停状态（暂停模式/白名单/请求状态）。
 * 与 Silicon 的同名类同构（同包简单名优先解析），避免与 mindustry.Vars 混淆。
 */
public class Vars {
    public static volatile Pause pause = new Pause("", true);

    /** 暂停模式：0=关闭，1=管理员，2=自定义白名单 */
    public static int pauseMode = 0;
    /** 自定义白名单（玩家名） */
    public static Seq<String> pauseWhitelist = new Seq<>();

    public static class Pause {
        String time;
        boolean complete;

        Pause(String time, boolean complete) {
            this.time = time;
            this.complete = complete;
        }

        Pause(String time) {
            this(time, false);
        }
    }
}

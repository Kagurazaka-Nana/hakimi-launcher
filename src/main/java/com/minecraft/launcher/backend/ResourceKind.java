package com.minecraft.launcher.backend;

/** 统一资源列表的分类种类。 */
public enum ResourceKind {

    MODS("Mods", java.util.List.of("全部", "性能优化", "冒险", "建筑", "科技", "魔法")),
    RESOURCE_PACK("资源包", java.util.List.of("全部", "写实", "卡通", "高对比", "低像素")),
    DATA_PACK("数据包", java.util.List.of("全部", "玩法", "地图", "配方", "战利品")),
    SHADER("光影", java.util.List.of("全部", "写实", "卡通", "性能向", "电影感")),
    MODPACK("整合包", java.util.List.of("全部", "科技", "魔法", "冒险", "硬核")),
    PLUGIN("插件", java.util.List.of("全部", "经济", "权限", "小游戏", "管理")),
    SERVER("服务器", java.util.List.of("全部", "生存", "空岛", "小游戏", "RPG"));

    private final String label;
    private final java.util.List<String> categories;

    ResourceKind(String label, java.util.List<String> categories) {
        this.label = label;
        this.categories = categories;
    }

    public String getLabel() {
        return label;
    }

    public java.util.List<String> getCategories() {
        return categories;
    }
}

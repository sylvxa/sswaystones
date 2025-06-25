package lol.sylvie.sswaystones.enums;

import net.minecraft.util.Formatting;

public enum Visibility {
    PUBLIC("gui.sswaystones.visibility.public", Formatting.GREEN),
    DISCOVERABLE("gui.sswaystones.visibility.discoverable", Formatting.GOLD),
    PRIVATE("gui.sswaystones.visibility.private", Formatting.RED);

    private final String displayName;
    private final Formatting formatting;

    Visibility(String displayName, Formatting formatting) {
        this.displayName = displayName;
        this.formatting = formatting;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Formatting getFormatting() {
        return formatting;
    }
}

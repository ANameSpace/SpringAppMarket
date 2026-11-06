package io.github.anamespace.userservice.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The immutable palette used for new avatars. Users may change their colour to
 * any hex value later, but new accounts start with one of these colours.
 */
public final class AvatarColors {

    public static final List<String> PALETTE = List.of(
            "#6B7280",
            "#EF4444",
            "#F59E0B",
            "#10B981",
            "#3B82F6",
            "#8B5CF6",
            "#EC4899",
            "#06B6D4"
    );

    public static final String DEFAULT = PALETTE.getFirst();

    private AvatarColors() {
    }

    public static String random() {
        return PALETTE.get(ThreadLocalRandom.current().nextInt(PALETTE.size()));
    }
}

package de.baum2dev.baum2.world;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Dev tool: renders the whole authored Heimgrund layout (zones, roads, POIs, stone-slot
 * anchors) into a PNG - pure {@link ZoneLayout} math, no chunks needed, so the full map
 * can be inspected without generating or walking it. Triggered by {@code /baum2 world map}
 * (op); writes heimgrund_map.png into the server's working directory.
 */
public final class MapExporter {

    private static final int RANGE = 520;   // blocks from center to render on each side
    private static final int STEP = 2;      // blocks per pixel

    public static File export(File directory) throws IOException {
        int size = RANGE * 2 / STEP;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int px = 0; px < size; px++) {
            for (int pz = 0; pz < size; pz++) {
                int x = -RANGE + px * STEP;
                int z = -RANGE + pz * STEP;
                image.setRGB(px, pz, colorAt(x, z));
            }
        }
        File out = new File(directory, "heimgrund_map.png");
        ImageIO.write(image, "png", out);
        return out;
    }

    private static int colorAt(int x, int z) {
        if (ZoneLayout.isHotspotApron(x, z)) {
            return 0xE0A030;  // amber: POI aprons
        }
        if (ZoneLayout.isPath(x, z)) {
            return 0xB07040;  // brown: roads
        }
        return switch (ZoneLayout.zoneAt(x, z)) {
            case CLEARING -> 0x88C070;
            case MEADOW -> ZoneLayout.isBeach(x, z) ? 0xD8CFA0 : 0x5E9446;
            case LAKE -> 0x3459B4;
            case DESERT -> 0xC9B873;
            case MOUNTAIN -> heightShade(x, z);
        };
    }

    /** Mountains shaded by height so the ramp/cliff/crest structure is visible. */
    private static int heightShade(int x, int z) {
        int height = ZoneLayout.surfaceHeight(x, z);
        int value = Math.min(230, 60 + (height - 70));
        return (value << 16) | (value << 8) | value;
    }

    private MapExporter() {
    }
}

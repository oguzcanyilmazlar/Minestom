package net.minestom.server.tag;

import net.minestom.server.coordinate.Vec;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TagListenTest {
    @Test
    public void basic() {
        var tag = Tag.Integer("coin");
        var handler = TagHandler.builder()
                .listen(tag, integer -> {
                    assertNotNull(integer);
                    return integer + 1;
                })
                .build();
        handler.setTag(tag, 5);
        assertEquals(6, handler.getTag(tag));
    }

    @Test
    public void restriction() {
        var tag = Tag.Integer("coin");
        var handler = TagHandler.builder()
                .listen(tag, integer -> {
                    throw new IllegalStateException();
                })
                .build();
        assertThrows(IllegalStateException.class, () -> handler.setTag(tag, 5));
        assertNull(handler.getTag(tag));
    }

    @Test
    public void view() {
        var tag = Tag.Structure("", Vec.class);
        var handler = TagHandler.builder()
                .listen(tag, vec -> {
                    assertNotNull(vec);
                    return vec.add(1);
                })
                .build();
        handler.setTag(tag, new Vec(1, 2, 3));
        assertEquals(new Vec(2, 3, 4), handler.getTag(tag));
    }

    @Test
    public void updateView() {
        var tag = Tag.Structure("", Vec.class);
        var handler = TagHandler.builder()
                .listen(tag, vec -> {
                    assertNotNull(vec);
                    return vec.add(1);
                })
                .build();
        handler.updateTag(tag, v -> new Vec(1, 2, 3));
        assertEquals(new Vec(2, 3, 4), handler.getTag(tag));
    }

    @Test
    public void update() {
        var tag = Tag.Integer("coin");
        var counter = new AtomicInteger();
        var handler = TagHandler.builder()
                .listen(tag, integer -> {
                    switch (counter.getAndIncrement()) {
                        case 0 -> assertEquals(5, integer);
                        case 1 -> assertEquals(6, integer);
                        case 2 -> assertNull(integer);
                        default -> fail();
                    }
                    return integer;
                })
                .build();
        // 0
        handler.updateTag(tag, integer -> 5);
        // 1
        handler.updateTag(tag, integer -> integer + 1);
        // 2
        handler.updateTag(tag, integer -> null);

        assertEquals(3, counter.get());
    }

    @Test
    public void setNbtConversion() {
        record Test(int coin) {
        }

        var tag1 = Tag.Integer("coin").path("path");
        var tag2 = Tag.Structure("path", Test.class);

        var counter = new AtomicInteger();
        var handler = TagHandler.builder()
                .listen(tag1, integer -> {
                    switch (counter.getAndIncrement()) {
                        case 0 -> assertEquals(5, integer);
                        case 1 -> assertEquals(7, integer);
                        default -> fail();
                    }
                    return integer;
                })
                .build();
        // 0
        handler.setTag(tag1, 5);
        assertEquals(5, handler.getTag(tag1));
        assertEquals(new Test(5), handler.getTag(tag2));
        // 1
        handler.setTag(tag2, new Test(7));
        assertEquals(7, handler.getTag(tag1));
        assertEquals(new Test(7), handler.getTag(tag2));

        assertEquals(2, counter.get());
    }

    @Test
    public void setNbtConversionPath() {
        record Test(int coin) {
        }

        var tag1 = Tag.Integer("coin").path("path", "path2", "path3");
        var tag2 = Tag.Structure("path3", Test.class).path("path", "path2");

        var counter = new AtomicInteger();
        var handler = TagHandler.builder()
                .listen(tag1, integer -> {
                    switch (counter.getAndIncrement()) {
                        case 0 -> assertEquals(5, integer);
                        case 1 -> assertEquals(7, integer);
                        default -> fail();
                    }
                    return integer;
                })
                .build();
        // 0
        handler.setTag(tag1, 5);
        assertEquals(5, handler.getTag(tag1));
        assertEquals(new Test(5), handler.getTag(tag2));
        // 1
        handler.setTag(tag2, new Test(7));
        assertEquals(7, handler.getTag(tag1));
        assertEquals(new Test(7), handler.getTag(tag2));

        assertEquals(2, counter.get());
    }

    @Test
    public void updateNbtConversion() {
        record Test(int coin) {
        }

        var tag1 = Tag.Integer("coin").path("path");
        var tag2 = Tag.Structure("path", Test.class);

        var counter = new AtomicInteger();
        var handler = TagHandler.builder()
                .listen(tag1, integer -> {
                    switch (counter.getAndIncrement()) {
                        case 0 -> assertEquals(5, integer);
                        case 1 -> assertEquals(7, integer);
                        default -> fail();
                    }
                    return integer;
                })
                .build();
        // 0
        handler.updateTag(tag1, integer -> 5);
        assertEquals(5, handler.getTag(tag1));
        assertEquals(new Test(5), handler.getTag(tag2));
        // 1
        handler.updateTag(tag2, test -> new Test(7));
        assertEquals(7, handler.getTag(tag1));
        assertEquals(new Test(7), handler.getTag(tag2));

        assertEquals(2, counter.get());
    }

    @Test
    public void updateNbtConversionPath() {
        record Test(int coin) {
        }

        var tag1 = Tag.Integer("coin").path("path", "path2", "path3");
        var tag2 = Tag.Structure("path3", Test.class).path("path", "path2");

        var counter = new AtomicInteger();
        var handler = TagHandler.builder()
                .listen(tag1, integer -> {
                    switch (counter.getAndIncrement()) {
                        case 0 -> assertEquals(5, integer);
                        case 1 -> assertEquals(7, integer);
                        default -> fail();
                    }
                    return integer;
                })
                .build();
        // 0
        handler.updateTag(tag1, integer -> 5);
        assertEquals(5, handler.getTag(tag1));
        assertEquals(new Test(5), handler.getTag(tag2));
        // 1
        handler.updateTag(tag2, test -> new Test(7));
        assertEquals(7, handler.getTag(tag1));
        assertEquals(new Test(7), handler.getTag(tag2));

        assertEquals(2, counter.get());
    }

    @Test
    public void setIncompatible() {
        var tagI = Tag.Integer("coin");
        var tagD = Tag.Double("coin");
        var handler = TagHandler.builder()
                .listen(tagI, integer -> {
                    assertNotNull(integer);
                    return integer;
                })
                .build();
        assertThrows(ClassCastException.class, () -> handler.setTag(tagD, 5d));
        assertNull(handler.getTag(tagI));
        assertNull(handler.getTag(tagD));

        handler.setTag(tagI, 5);
        assertEquals(5, handler.getTag(tagI));
        assertNull(handler.getTag(tagD));
    }

    @Test
    public void updateIncompatible() {
        var tagI = Tag.Integer("coin");
        var tagD = Tag.Double("coin");
        var handler = TagHandler.builder()
                .listen(tagI, integer -> {
                    assertNotNull(integer);
                    return integer + 1;
                })
                .build();
        assertThrows(ClassCastException.class, () -> handler.updateTag(tagD, d -> 5d));
        assertNull(handler.getTag(tagI));
        assertNull(handler.getTag(tagD));

        handler.updateTag(tagI, i -> 5);
        assertEquals(6, handler.getTag(tagI));
        assertNull(handler.getTag(tagD));
    }

    @Test
    public void copyErase() {
        var tag = Tag.Integer("coin");
        var handler = TagHandler.builder()
                .listen(tag, integer -> integer + 1)
                .build();
        handler.setTag(tag, 5);
        assertEquals(6, handler.getTag(tag));

        handler = handler.copy();
        handler.setTag(tag, 10);
        assertEquals(10, handler.getTag(tag), "Listeners should not be preserved during copy");
    }
}

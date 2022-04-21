package net.minestom.server.api;

import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public final class RandomUtils {
    public static NBTCompound randomizeCompound(int count) {
        return randomizeCompound(count, new AtomicInteger(0));
    }

    private static NBTCompound randomizeCompound(int count, AtomicInteger index) {
        var random = new java.util.Random();
        MutableNBTCompound result = new MutableNBTCompound();
        for (; index.get() < count; index.getAndIncrement()) {
            final String key = random.nextInt(100) > 5 ? RandomUtils.nextString(7, random) : String.valueOf(random.nextInt(100));
            final NBTType<?> type = NBTType.byIndex(random.nextInt(12) + 1);
            final NBT nbt = randomNbt(type, random, count, index);
            result.put(key, nbt);
        }
        return result.toCompound();
    }

    private static NBT randomNbt(NBTType<?> type, Random random, int count, AtomicInteger index) {
        if (type == NBTType.TAG_Byte) {
            return NBT.Byte(random.nextInt(Byte.MAX_VALUE));
        } else if (type == NBTType.TAG_Short) {
            return NBT.Short(random.nextInt(Short.MAX_VALUE));
        } else if (type == NBTType.TAG_Int) {
            return NBT.Int(random.nextInt());
        } else if (type == NBTType.TAG_Long) {
            return NBT.Long(random.nextLong());
        } else if (type == NBTType.TAG_Float) {
            return NBT.Float(random.nextFloat());
        } else if (type == NBTType.TAG_Double) {
            return NBT.Double(random.nextDouble());
        } else if (type == NBTType.TAG_Byte_Array) {
            final byte[] array = RandomUtils.nextBytes(10, random);
            return NBT.ByteArray(array);
        } else if (type == NBTType.TAG_String) {
            final String value = random.nextInt(100) > 5 ?
                    RandomUtils.nextString(7, random) : String.valueOf(random.nextInt(100));
            return NBT.String(value);
        } else if (type == NBTType.TAG_List) {
            final int length = random.nextInt(10);
            List<NBT> tmpList = new ArrayList<>(length);
            final NBTType<?> listType = randomNbtType(random);
            for (int i = 0; i < length; i++) tmpList.add(randomNbt(listType, random, count, index));
            return NBT.List(listType, tmpList);
        } else if (type == NBTType.TAG_Compound) {
            return randomizeCompound(count, index);
        } else if (type == NBTType.TAG_Int_Array) {
            final int length = random.nextInt(10);
            final int[] array = new int[length];
            for (int i = 0; i < length; i++) array[i] = random.nextInt();
            return NBT.IntArray(array);
        } else if (type == NBTType.TAG_Long_Array) {
            final int length = random.nextInt(10);
            final long[] array = new long[length];
            for (int i = 0; i < length; i++) array[i] = random.nextLong();
            return NBT.LongArray(array);
        }
        throw new IllegalArgumentException("Unsupported type: " + type);
    }

    private static NBTType<?> randomNbtType(Random random) {
        return NBTType.byIndex(random.nextInt(12) + 1);
    }

    public static byte[] nextBytes(final int count, Random random) {
        final byte[] result = new byte[count];
        random.nextBytes(result);
        return result;
    }

    public static String nextString(final int length, Random random) {
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        StringBuilder buffer = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int randomLimitedInt = leftLimit + (int)
                    (random.nextFloat() * (rightLimit - leftLimit + 1));
            buffer.append((char) randomLimitedInt);
        }
        return buffer.toString();
    }
}

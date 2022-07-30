package net.minestom.server.tag;

import net.minestom.server.utils.PropertyUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;
import org.jglrxavpok.hephaistos.nbt.NBT;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.jglrxavpok.hephaistos.nbt.NBTCompoundLike;
import org.jglrxavpok.hephaistos.nbt.NBTType;
import org.jglrxavpok.hephaistos.nbt.mutable.MutableNBTCompound;

import java.lang.invoke.VarHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

final class TagHandlerImpl implements TagHandler {
    private static final boolean CACHE_ENABLE = PropertyUtils.getBoolean("minestom.tag-handler-cache", true);
    static final Serializers.Entry<Node, NBTCompound> NODE_SERIALIZER = new Serializers.Entry<>(NBTType.TAG_Compound, entries -> fromCompound(entries).root, Node::compound, true);

    private final Node root;
    private volatile Node copy;

    // Options
    private final Map<String, ListenerEntry<?>> listeners;

    TagHandlerImpl() {
        this.root = new Node();
        this.listeners = Map.of();
    }

    // Builder constructor
    TagHandlerImpl(BuilderImpl builder) {
        this.root = new Node();
        this.listeners = Map.copyOf(builder.listeners);
    }

    // Copy constructor
    TagHandlerImpl(TagHandlerImpl previous) {
        this.root = previous.root.copy(null);
        // Options are not preserved on copy
        this.listeners = Map.of();
    }

    static TagHandlerImpl fromCompound(NBTCompoundLike compoundLike) {
        final NBTCompound compound = compoundLike.toCompound();
        TagHandlerImpl handler = new TagHandlerImpl();
        TagNbtSeparator.separate(compound, entry -> handler.setTag(entry.tag(), entry.value()));
        handler.root.compound = compound;
        return handler;
    }

    @Override
    public <T> @UnknownNullability T getTag(@NotNull Tag<T> tag) {
        VarHandle.fullFence();
        return root.getTag(tag);
    }

    private <T> UnaryOperator<T> findListener(Tag<T> tag, T value) {
        var listeners = this.listeners;
        if (listeners.isEmpty()) return null;

        final String pathString = tag.pathString();
        ListenerEntry<T> result = (ListenerEntry<T>) listeners.get(pathString);
        if (result != null) {
            return result.operator();
        }

        final NBT nbt = tag.entry.write(value);
        if (nbt instanceof NBTCompound compound) {
            for (var entry : compound.getEntries()) {
                // TODO recursive
                String test = pathString + "." + entry.getKey();
                final ListenerEntry<T> listener = (ListenerEntry<T>) listeners.get(test);
                if (listener != null) {
                    return t -> {
                        final T converted = listener.tag().entry.read(entry.getValue());
                        final T updated = listener.operator().apply(converted);

                        final NBTCompound finalNbt = compound.withEntries(Map.entry(entry.getKey(), listener.tag.entry.write(updated)));
                        return tag.entry.read(finalNbt);
                    };
                }
            }
        }
        return null;
    }

    @Override
    public <T> void setTag(@NotNull Tag<T> tag, @Nullable T value) {
        // Handle view tags
        if (tag.isView()) {
            synchronized (this) {
                Node syncNode = traversePathWrite(root, tag, value != null);
                if (syncNode != null) {
                    final UnaryOperator<T> listener = findListener(tag, value);
                    final ListenerResult<T> result = unsafeListening(syncNode, tag, value, listener);
                    syncNode = result.node();
                    value = result.value();
                    syncNode.updateContent(value != null ? (NBTCompound) tag.entry.write(value) : NBTCompound.EMPTY);
                    syncNode.invalidate();
                }
            }
            return;
        }
        // Normal tag
        VarHandle.fullFence();
        Node node = traversePathWrite(root, tag, value != null);
        if (node == null)
            return; // Tried to remove an absent tag. Do nothing

        final UnaryOperator<T> listener = findListener(tag, value);
        StaticIntMap<Entry<?>> entries = node.entries;
        Entry previous;
        if (value != null && (previous = entries.get(tag.index)) != null && listener == null && previous.tag.shareValue(tag)) {
            // Fast lock-free update if a compatible entry is already present and there is no listener
            previous.updateValue(tag.copyValue(value));
        } else {
            // Fallback to lock
            synchronized (this) {
                node = traversePathWrite(root, tag, value != null);
                final ListenerResult<T> result = unsafeListening(node, tag, value, listener);
                node = result.node();
                value = result.value();
                if (value != null) {
                    node.entries.put(tag.index, valueToEntry(node, tag, value));
                } else {
                    if (node != null) node.entries.remove(tag.index);
                }
            }
        }
        if (node != null) node.invalidate();
    }

    private <T> ListenerResult<T> unsafeListening(Node node, Tag<T> tag, @Nullable T value, @Nullable UnaryOperator<T> listener) {
        if (listener != null) {
            final T tmpValue = listener.apply(value);
            // Check if nullability changed
            if (value == null && tmpValue != null || value != null && tmpValue == null) {
                node = traversePathWrite(root, tag, tmpValue != null);
            }
            value = tmpValue;
        }
        return new ListenerResult<>(node, value);
    }

    record ListenerResult<T>(Node node, T value) {
    }

    @Override
    public <T> void updateTag(@NotNull Tag<T> tag, @NotNull UnaryOperator<@UnknownNullability T> value) {
        updateTag0(tag, value, false);
    }

    @Override
    public <T> @UnknownNullability T updateAndGetTag(@NotNull Tag<T> tag, @NotNull UnaryOperator<@UnknownNullability T> value) {
        return updateTag0(tag, value, false);
    }

    @Override
    public <T> @UnknownNullability T getAndUpdateTag(@NotNull Tag<T> tag, @NotNull UnaryOperator<@UnknownNullability T> value) {
        return updateTag0(tag, value, true);
    }

    private synchronized <T> T updateTag0(@NotNull Tag<T> tag, @NotNull UnaryOperator<T> value, boolean returnPrevious) {
        Node node = traversePathWrite(root, tag, true);
        if (tag.isView()) {
            final T previousValue = tag.read(node.compound());
            T newValue = value.apply(previousValue);

            final UnaryOperator<T> listener = findListener(tag, newValue);
            final ListenerResult<T> result = unsafeListening(node, tag, newValue, listener);
            node = result.node();
            newValue = result.value();

            node.updateContent((NBTCompoundLike) tag.entry.write(newValue));
            node.invalidate();
            return returnPrevious ? previousValue : newValue;
        }

        final int tagIndex = tag.index;
        final Entry previousEntry = node.entries.get(tagIndex);
        final T previousValue;
        if (previousEntry != null) {
            previousValue = (T) previousEntry.castValue(tag);
        } else {
            previousValue = tag.createDefault();
        }
        T newValue = value.apply(previousValue);

        // Handle listening
        final UnaryOperator<T> listener = findListener(tag, newValue);
        final ListenerResult<T> result = unsafeListening(node, tag, newValue, listener);
        node = result.node();
        newValue = result.value();

        // Update node
        if (newValue != null) node.entries.put(tagIndex, valueToEntry(node, tag, newValue));
        else node.entries.remove(tagIndex);
        node.invalidate();
        return returnPrevious ? previousValue : newValue;
    }

    @Override
    public @NotNull TagReadable readableCopy() {
        Node copy = this.copy;
        if (copy == null) {
            synchronized (this) {
                this.copy = copy = root.copy(null);
            }
        }
        return copy;
    }

    @Override
    public synchronized @NotNull TagHandler copy() {
        return new TagHandlerImpl(this);
    }

    @Override
    public synchronized void updateContent(@NotNull NBTCompoundLike compound) {
        this.root.updateContent(compound);
    }

    @Override
    public @NotNull NBTCompound asCompound() {
        VarHandle.fullFence();
        return root.compound();
    }

    private static Node traversePathRead(Node node, Tag<?> tag) {
        final Tag.PathEntry[] paths = tag.path;
        if (paths == null) return node;
        for (var path : paths) {
            final Entry<?> entry = node.entries.get(path.index());
            if (entry == null || (node = entry.toNode()) == null)
                return null;
        }
        return node;
    }

    @Contract("_, _, true -> !null")
    private Node traversePathWrite(Node root, Tag<?> tag,
                                   boolean present) {
        final Tag.PathEntry[] paths = tag.path;
        if (paths == null) return root;
        Node local = root;
        for (Tag.PathEntry path : paths) {
            final int pathIndex = path.index();
            final Entry<?> entry = local.entries.get(pathIndex);
            if (entry != null && entry.tag.entry.isPath()) {
                // Existing path, continue navigating
                final Node tmp = (Node) entry.value;
                assert tmp.parent == local : "Path parent is invalid: " + tmp.parent + " != " + local;
                local = tmp;
            } else {
                if (!present) return null;
                synchronized (this) {
                    var synEntry = local.entries.get(pathIndex);
                    if (synEntry != null && synEntry.tag.entry.isPath()) {
                        // Existing path, continue navigating
                        final Node tmp = (Node) synEntry.value;
                        assert tmp.parent == local : "Path parent is invalid: " + tmp.parent + " != " + local;
                        local = tmp;
                        continue;
                    }

                    // Empty path, create a new handler.
                    // Slow path is taken if the entry comes from a Structure tag, requiring conversion from NBT
                    Node tmp = local;
                    local = new Node(tmp);
                    if (synEntry != null && synEntry.updatedNbt() instanceof NBTCompound compound) {
                        local.updateContent(compound);
                    }
                    tmp.entries.put(pathIndex, Entry.makePathEntry(path.name(), local));
                }
            }
        }
        return local;
    }

    private <T> Entry<?> valueToEntry(Node parent, Tag<T> tag, @NotNull T value) {
        if (value instanceof NBT nbt) {
            if (nbt instanceof NBTCompound compound) {
                final TagHandlerImpl handler = fromCompound(compound);
                return Entry.makePathEntry(tag, new Node(parent, handler.root.entries));
            } else {
                final var nbtEntry = TagNbtSeparator.separateSingle(tag.getKey(), nbt);
                return new Entry<>(nbtEntry.tag(), nbtEntry.value());
            }
        } else {
            return new Entry<>(tag, tag.copyValue(value));
        }
    }

    final class Node implements TagReadable {
        final Node parent;
        final StaticIntMap<Entry<?>> entries;
        NBTCompound compound;

        public Node(Node parent, StaticIntMap<Entry<?>> entries) {
            this.parent = parent;
            this.entries = entries;
        }

        Node(Node parent) {
            this(parent, new StaticIntMap.Array<>());
        }

        Node() {
            this(null);
        }

        @Override
        public <T> @UnknownNullability T getTag(@NotNull Tag<T> tag) {
            final Node node = traversePathRead(this, tag);
            if (node == null)
                return tag.createDefault(); // Must be a path-able entry, but not present
            if (tag.isView()) return tag.read(node.compound());

            final StaticIntMap<Entry<?>> entries = node.entries;
            final Entry<?> entry = entries.get(tag.index);
            if (entry == null)
                return tag.createDefault(); // Not present
            if (entry.tag.shareValue(tag)) {
                // The tag used to write the entry is compatible with the one used to get
                // return the value directly
                //noinspection unchecked
                return (T) entry.value;
            }
            // Value must be parsed from nbt if the tag is different
            final NBT nbt = entry.updatedNbt();
            final Serializers.Entry<T, NBT> serializerEntry = tag.entry;
            final NBTType<NBT> type = serializerEntry.nbtType();
            return type == null || type == nbt.getID() ? serializerEntry.read(nbt) : tag.createDefault();
        }

        void updateContent(@NotNull NBTCompoundLike compoundLike) {
            final NBTCompound compound = compoundLike.toCompound();
            final TagHandlerImpl converted = fromCompound(compound);
            this.entries.updateContent(converted.root.entries);
            this.compound = compound;
        }

        NBTCompound compound() {
            NBTCompound compound;
            if (!CACHE_ENABLE || (compound = this.compound) == null) {
                MutableNBTCompound tmp = new MutableNBTCompound();
                this.entries.forValues(entry -> {
                    final Tag tag = entry.tag;
                    final NBT nbt = entry.updatedNbt();
                    if (!tag.entry.isPath() || !((NBTCompound) nbt).isEmpty()) {
                        tmp.put(tag.getKey(), nbt);
                    }
                });
                this.compound = compound = tmp.toCompound();
            }
            return compound;
        }

        @Contract("null -> !null")
        Node copy(Node parent) {
            MutableNBTCompound tmp = new MutableNBTCompound();
            Node result = new Node(parent, new StaticIntMap.Array<>());
            StaticIntMap<Entry<?>> entries = result.entries;
            this.entries.forValues(entry -> {
                Tag tag = entry.tag;
                Object value = entry.value;
                NBT nbt;
                if (value instanceof Node node) {
                    Node copy = node.copy(result);
                    if (copy == null)
                        return; // Empty node
                    value = copy;
                    nbt = copy.compound;
                    assert nbt != null : "Node copy should also compute the compound";
                } else {
                    nbt = entry.updatedNbt();
                }

                tmp.put(tag.getKey(), nbt);
                entries.put(tag.index, valueToEntry(result, tag, value));
            });
            if (tmp.isEmpty() && parent != null)
                return null; // Empty child node
            result.compound = tmp.toCompound();
            return result;
        }

        void invalidate() {
            Node tmp = this;
            do tmp.compound = null;
            while ((tmp = tmp.parent) != null);
            TagHandlerImpl.this.copy = null;
        }
    }

    private static final class Entry<T> {
        private final Tag<T> tag;
        T value;
        NBT nbt;

        Entry(Tag<T> tag, T value) {
            this.tag = tag;
            this.value = value;
        }

        static Entry<?> makePathEntry(String path, Node node) {
            return new Entry<>(Tag.tag(path, NODE_SERIALIZER), node);
        }

        static Entry<?> makePathEntry(Tag<?> tag, Node node) {
            return makePathEntry(tag.getKey(), node);
        }

        <T> T castValue(Tag<T> tag) {
            final Object value = this.value;
            if (value instanceof Node n) {
                final NBTCompound compound = NBT.Compound(Map.of(tag.getKey(), n.compound()));
                return tag.read(compound);
            } else {
                return (T) value;
            }
        }

        NBT updatedNbt() {
            if (tag.entry.isPath()) return ((Node) value).compound();
            NBT nbt = this.nbt;
            if (nbt == null) this.nbt = nbt = tag.entry.write(value);
            return nbt;
        }

        void updateValue(T value) {
            assert !tag.entry.isPath();
            this.value = value;
            this.nbt = null;
        }

        Node toNode() {
            if (tag.entry.isPath()) return (Node) value;
            if (updatedNbt() instanceof NBTCompound compound) {
                // Slow path forcing a conversion of the structure to NBTCompound
                // TODO should the handler be cached inside the entry?
                return fromCompound(compound).root;
            }
            // Entry is not path-able
            return null;
        }
    }

    record ListenerEntry<T>(Tag<T> tag, UnaryOperator<T> operator) {
    }

    static final class BuilderImpl implements TagHandler.Builder {
        private final Map<String, ListenerEntry<?>> listeners = new HashMap<>();

        @Override
        public <T> @NotNull Builder listen(@NotNull Tag<T> tag, @NotNull UnaryOperator<@Nullable T> value) {
            this.listeners.put(tag.pathString(), new ListenerEntry<>(tag, value));
            return this;
        }

        @Override
        public @NotNull TagHandler build() {
            return new TagHandlerImpl(this);
        }
    }
}

package net.minestom.server.tag;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.util.List;
import java.util.Optional;

@ApiStatus.Experimental
public interface TagDatabase {
    SelectQuery<NBTCompound> SELECT_ALL = selectAll().build();

    void insert(@NotNull TagHandler handler);

    int execute(@NotNull UpdateQuery query);

    int execute(@NotNull DeleteQuery query);

    <T> List<T> execute(@NotNull SelectQuery<T> query);

    default void update(@NotNull Condition condition, @NotNull TagHandler handler) {
        execute(update().where(condition).setAll(handler).build());
    }

    default <T> void update(@NotNull Tag<T> tag, @NotNull T value, @NotNull TagHandler handler) {
        execute(update().where(Condition.eq(tag, value)).setAll(handler).build());
    }

    default <T> @NotNull Optional<@NotNull NBTCompound> findFirst(@NotNull Tag<T> tag, @NotNull T value) {
        final SelectQuery<NBTCompound> query = selectAll().where(Condition.eq(tag, value)).limit(1).build();
        final List<NBTCompound> res = execute(query);
        return res.isEmpty() ? Optional.empty() : Optional.of(res.get(0));
    }

    static <T> @NotNull SelectBuilder<T> select(@NotNull Tag<T> tag) {
        return new TagDatabaseImpl.SelectBuilder<>(tag);
    }

    static @NotNull SelectBuilder<NBTCompound> selectAll() {
        return select(Tag.View(TagSerializer.COMPOUND));
    }

    static @NotNull UpdateBuilder update() {
        return new TagDatabaseImpl.UpdateBuilder();
    }

    static @NotNull DeleteBuilder delete() {
        return new TagDatabaseImpl.DeleteBuilder();
    }

    sealed interface SelectQuery<T> permits TagDatabaseImpl.Select {
        @NotNull Tag<T> selector();

        @NotNull Condition condition();

        @Unmodifiable
        @NotNull List<@NotNull Sorter> sorters();

        int limit();
    }

    sealed interface SelectBuilder<T> permits TagDatabaseImpl.SelectBuilder {
        @NotNull SelectBuilder<T> where(@NotNull Condition condition);

        @NotNull SelectBuilder<T> orderByAsc(@NotNull Tag<?> tag);

        @NotNull SelectBuilder<T> orderByDesc(@NotNull Tag<?> tag);

        @NotNull SelectBuilder<T> limit(int limit);

        @NotNull SelectQuery<T> build();
    }

    sealed interface UpdateQuery permits TagDatabaseImpl.Update {
        @NotNull Condition condition();

        @Unmodifiable
        @NotNull List<@NotNull Operation> operations();
    }

    sealed interface UpdateBuilder permits TagDatabaseImpl.UpdateBuilder {
        @NotNull UpdateBuilder where(@NotNull Condition condition);

        <T> @NotNull UpdateBuilder set(@NotNull Tag<T> tag, @NotNull T value);

        default @NotNull UpdateBuilder setAll(@NotNull TagHandler tagHandler) {
            return set(Tag.View(TagSerializer.COMPOUND), tagHandler.asCompound());
        }

        @NotNull UpdateQuery build();
    }

    sealed interface DeleteQuery permits TagDatabaseImpl.Delete {
        @NotNull Condition condition();
    }

    sealed interface DeleteBuilder permits TagDatabaseImpl.DeleteBuilder {
        @NotNull DeleteBuilder where(@NotNull Condition condition);

        @NotNull DeleteQuery build();
    }

    sealed interface Condition permits Condition.Eq {
        static <T> Condition eq(@NotNull Tag<T> tag, @NotNull T value) {
            return new TagDatabaseImpl.ConditionEq<>(tag, value);
        }

        sealed interface Eq<T> extends Condition permits TagDatabaseImpl.ConditionEq {
            @NotNull Tag<T> tag();

            @NotNull T value();
        }
    }

    sealed interface Operation permits Operation.Set {
        static <T> Operation set(@NotNull Tag<T> tag, @NotNull T value) {
            return new TagDatabaseImpl.OperationSet<>(tag, value);
        }

        sealed interface Set<T> extends Operation permits TagDatabaseImpl.OperationSet {
            @NotNull Tag<T> tag();

            @NotNull T value();
        }
    }

    sealed interface Sorter permits TagDatabaseImpl.Sorter {
        @NotNull Tag<?> tag();

        @NotNull SortOrder sortOrder();
    }

    enum SortOrder {
        ASCENDING,
        DESCENDING
    }
}

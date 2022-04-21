package net.minestom.server.tag;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class TagDatabaseImpl {
    record Select<T>(Tag<T> selector,
                     TagDatabase.Condition condition,
                     List<TagDatabase.Sorter> sorters,
                     int limit) implements TagDatabase.SelectQuery<T> {
        Select {
            sorters = List.copyOf(sorters);
        }
    }

    record Update(TagDatabase.Condition condition,
                  List<TagDatabase.Operation> operations) implements TagDatabase.UpdateQuery {
        Update {
            operations = List.copyOf(operations);
        }
    }

    record Delete(TagDatabase.Condition condition) implements TagDatabase.DeleteQuery {
    }

    static final class SelectBuilder<T> implements TagDatabase.SelectBuilder<T> {
        private final Tag<T> selector;
        private TagDatabase.Condition condition;
        private final List<TagDatabase.Sorter> sorters = new ArrayList<>();
        private int limit = -1;

        SelectBuilder(Tag<T> selector) {
            this.selector = selector;
        }

        @Override
        public TagDatabase.@NotNull SelectBuilder<T> where(TagDatabase.@NotNull Condition condition) {
            this.condition = condition;
            return this;
        }

        @Override
        public TagDatabase.@NotNull SelectBuilder<T> orderByAsc(@NotNull Tag<?> tag) {
            this.sorters.add(new Sorter(tag, TagDatabase.SortOrder.ASCENDING));
            return this;
        }

        @Override
        public TagDatabase.@NotNull SelectBuilder<T> orderByDesc(@NotNull Tag<?> tag) {
            this.sorters.add(new Sorter(tag, TagDatabase.SortOrder.DESCENDING));
            return this;
        }

        @Override
        public TagDatabase.@NotNull SelectBuilder<T> limit(int limit) {
            this.limit = limit;
            return this;
        }

        @Override
        public TagDatabase.@NotNull SelectQuery<T> build() {
            return new Select<>(selector, condition, sorters, limit);
        }
    }

    static final class UpdateBuilder implements TagDatabase.UpdateBuilder {
        private TagDatabase.Condition condition;
        private final List<TagDatabase.Operation> operations = new ArrayList<>();

        @Override
        public TagDatabase.@NotNull UpdateBuilder where(TagDatabase.@NotNull Condition condition) {
            this.condition = condition;
            return this;
        }

        @Override
        public <T> TagDatabase.@NotNull UpdateBuilder set(@NotNull Tag<T> tag, @NotNull T value) {
            this.operations.add(new OperationSet<>(tag, value));
            return this;
        }

        @Override
        public TagDatabase.@NotNull UpdateQuery build() {
            return new Update(condition, operations);
        }
    }

    static final class DeleteBuilder implements TagDatabase.DeleteBuilder {
        private TagDatabase.Condition condition;

        @Override
        public TagDatabase.@NotNull DeleteBuilder where(TagDatabase.@NotNull Condition condition) {
            this.condition = condition;
            return this;
        }

        @Override
        public TagDatabase.@NotNull DeleteQuery build() {
            return new Delete(condition);
        }
    }

    record ConditionEq<T>(Tag<T> tag, T value) implements TagDatabase.Condition.Eq<T> {
    }

    record OperationSet<T>(Tag<T> tag, T value) implements TagDatabase.Operation.Set<T> {
    }

    record Sorter(Tag<?> tag, TagDatabase.SortOrder sortOrder) implements TagDatabase.Sorter {
    }
}

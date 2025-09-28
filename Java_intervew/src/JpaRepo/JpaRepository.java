package JpaRepo;

import Annotation.Column;
import Annotation.Entity;
import Annotation.Id;
import Annotation.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class JpaRepository<E, T> implements CRUDRepository<E, T> {
    @Override
    public void save(E entity) throws SQLException {
        Objects.requireNonNull(entity, "Entity must not be null");
        Class<E> clazz = this.getEntityClass();
        validateEntityClass(clazz);

        Map<String, Object> columnValues = extractColumnValues(entity, true);
        String tableName = resolveTableName(clazz);

        if (columnValues.isEmpty()) {
            throw new IllegalStateException("No columns mapped for entity " + clazz.getName());
        }

        StringBuilder columnsBuilder = new StringBuilder();
        StringBuilder placeholdersBuilder = new StringBuilder();
        int index = 0;
        for (String column : columnValues.keySet()) {
            if (index > 0) {
                columnsBuilder.append(", ");
                placeholdersBuilder.append(", ");
            }
            columnsBuilder.append(column);
            placeholdersBuilder.append("?");
            index++;
        }

        String sql = String.format("INSERT INTO %s (%s) VALUES (%s);", tableName, columnsBuilder, placeholdersBuilder);
        System.out.println(sql);
        System.out.println("Parameters: " + new ArrayList<>(columnValues.values()));

        try (Connection connection = DriverManager.getConnection("");
             PreparedStatement ps = connection.prepareStatement(sql)) {

            int i = 1;
            for (Object val : columnValues.values()) {
                ps.setObject(i++, val);
            }
            ps.executeUpdate();
        }
    }

    @Override
    public void findById(T id) {
        Objects.requireNonNull(id, "Identifier must not be null");
        Class<E> clazz = this.getEntityClass();
        validateEntityClass(clazz);

        Field idField = resolveIdField(clazz);
        String idColumn = resolveColumnName(idField);
        String tableName = resolveTableName(clazz);

        String sql = String.format("SELECT * FROM %s WHERE %s = ?;", tableName, idColumn);
        System.out.println(sql);
        System.out.println("Parameters: [" + id + "]");

    }

    @Override
    public void update(E entity) {
        Objects.requireNonNull(entity, "Entity must not be null");
        Class<E> clazz = this.getEntityClass();
        validateEntityClass(clazz);

        Field idField = resolveIdField(clazz);
        Object idValue = Objects.requireNonNull(extractFieldValue(entity, idField),
                "Identifier value must not be null");
        String idColumn = resolveColumnName(idField);
        Map<String, Object> columnValues = extractColumnValues(entity, false);
        String tableName = resolveTableName(clazz);

        List<String> assignments = new ArrayList<>();
        for (String column : columnValues.keySet()) {
            if (!column.equals(idColumn)) {
                assignments.add(column + " = ?");
            }
        }

        if (assignments.isEmpty()) {
            throw new IllegalStateException("No updatable columns mapped for entity " + clazz.getName());
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s = ?;", tableName, String.join(", ", assignments), idColumn);
        List<Object> parameters = new ArrayList<>(columnValues.values());
        parameters.add(idValue);
        System.out.println(sql);
        System.out.println("Parameters: " + parameters);
    }

    @Override
    public void deleteById(T id) {
        Objects.requireNonNull(id, "Identifier must not be null");
        Class<E> clazz = this.getEntityClass();
        validateEntityClass(clazz);

        Field idField = resolveIdField(clazz);
        String idColumn = resolveColumnName(idField);
        String tableName = resolveTableName(clazz);

        String sql = String.format("DELETE FROM %s WHERE %s = ?;", tableName, idColumn);
        System.out.println(sql);
        System.out.println("Parameters: [" + id + "]");
    }

    public abstract Class<E> getEntityClass();


    /**
     *
     * @param entity
     * @param includeIdentifier
     * @return : duyệt các trường có @Column/@Id (kể cả lớp cha), lấy tên cột và giá trị tương ứng, có thể loại trừ trường khóa chính khi cần.
     */
    private Map<String, Object> extractColumnValues(E entity, boolean includeIdentifier) {
        Map<String, Object> values = new LinkedHashMap<>();
        Class<?> clazz = entity.getClass();
        Field idField = resolveIdField(clazz);
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!field.isAnnotationPresent(Column.class) && !field.isAnnotationPresent(Id.class)) {
                    continue;
                }
                boolean isId = field.equals(idField);
                if (!includeIdentifier && isId) {
                    continue;
                }
                String columnName = resolveColumnName(field);
                Object value = extractFieldValue(entity, field);
                values.put(columnName, value);
            }
            current = current.getSuperclass();
        }
        return values;
    }

    private Object extractFieldValue(E entity, Field field) {
        try {
            boolean accessible = field.canAccess(entity);
            if (!accessible) {
                field.setAccessible(true);
            }
            Object value = field.get(entity);
            if (!accessible) {
                field.setAccessible(false);
            }
            return value;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Cannot access field '" + field.getName() + "'", e);
        }
    }

    private void validateEntityClass(Class<?> clazz) {
        if (clazz == null) {
            throw new IllegalStateException("Entity class must not be null");
        }
        if (!clazz.isAnnotationPresent(Entity.class)) {
            throw new IllegalStateException("Class " + clazz.getName() + " is not annotated with @Entity");
        }
    }

    private Field resolveIdField(Class<?> clazz) {
        Field idField = null;
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    if (idField != null) {
                        throw new IllegalStateException("Multiple @Id fields found in " + clazz.getName());
                    }
                    idField = field;
                }
            }
            current = current.getSuperclass();
        }
        if (idField == null) {
            throw new IllegalStateException("No field annotated with @Id found in " + clazz.getName());
        }
        return idField;
    }

    private String resolveTableName(Class<?> clazz) {
        Table table = clazz.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return clazz.getSimpleName();
    }

    private String resolveColumnName(Field field) {
        Id id = field.getAnnotation(Id.class);
        if (id != null && !id.name().isEmpty()) {
            return id.name();
        }
        Column column = field.getAnnotation(Column.class);
        if (column != null && !column.name().isEmpty()) {
            return column.name();
        }
        return field.getName();
    }
}

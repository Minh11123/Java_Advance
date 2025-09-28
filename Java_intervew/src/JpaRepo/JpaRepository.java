package JpaRepo;

public abstract class JpaRepository<E,T> implements CRUDRepository<E, T> {
    @Override
    public void save(E entity) {
        Class<E> clazz = this.getEntityClass();
        String tableName = clazz.getSimpleName();
    }

    @Override
    public void findById(T id) {

    }

    @Override
    public void update(E entity) {

    }

    @Override
    public void deleteById(T id) {

    }
    public abstract Class<E> getEntityClass();
}

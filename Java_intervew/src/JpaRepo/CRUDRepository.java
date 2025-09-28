package JpaRepo;

public interface CRUDRepository<E,T> {
    public void save(E entity);
    public void findById(T id);

    public void update(E entity);
    public void deleteById(T id);
}

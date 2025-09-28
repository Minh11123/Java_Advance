package JpaRepo;

import java.sql.SQLException;

public interface CRUDRepository<E,T> {
    public void save(E entity) throws SQLException;
    public void findById(T id);

    public void update(E entity);
    public void deleteById(T id);
}

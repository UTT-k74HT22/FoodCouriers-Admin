package com.utt.foodcouriers_admin.data.repository.base;

import com.utt.foodcouriers_admin.data.common.RepositoryCallback;

import java.util.List;

/**
 * Repository interface for CRUD operations
 * Edit by: DungHD
 * Edit data: 2026/04/06
 * @param <T> type of object
 * @param <R> type of request
 */
public interface CrudRepository<T, R> {
    /**
     * Get all items
     * @param callback callback
     */
    void getAll(RepositoryCallback<List<T>> callback);

    /**
     * Get item by id
     * @param id id
     * @param callback callback
     */
    void getById(String id, RepositoryCallback<T> callback);

    /**
     * Create item
     * @param request request
     * @param callback callback
     */
    void create(R request, RepositoryCallback<T> callback);

    /**
     * Update item
     * @param id id
     * @param request request
     * @param callback callback
     */
    void update(String id, R request, RepositoryCallback<T> callback);

    /**
     * Delete item
     * @param id id
     * @param callback callback
     */
    void delete(String id, RepositoryCallback<Void> callback);
}

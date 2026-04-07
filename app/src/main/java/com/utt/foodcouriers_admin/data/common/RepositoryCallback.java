package com.utt.foodcouriers_admin.data.common;

public interface RepositoryCallback<T> {
    void onComplete(BaseResponse<T> response);
}

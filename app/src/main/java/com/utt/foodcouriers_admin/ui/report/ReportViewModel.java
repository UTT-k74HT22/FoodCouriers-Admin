package com.utt.foodcouriers_admin.ui.report;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.utt.foodcouriers_admin.data.model.DailyStat;
import com.utt.foodcouriers_admin.data.repository.ReportRepository;

import java.util.List;

public class ReportViewModel extends ViewModel {

    private final ReportRepository repository;
    private final MutableLiveData<List<DailyStat>> reportData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReportViewModel() {
        this.repository = ReportRepository.getInstance();
    }

    public LiveData<List<DailyStat>> getReportData() { return reportData; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }

    public void loadReport(String fromDate, String toDate) {
        isLoading.setValue(true);
        
        repository.getReportData(fromDate, toDate, new ReportRepository.ReportCallback() {
            @Override
            public void onSuccess(List<DailyStat> stats) {
                isLoading.postValue(false);
                reportData.postValue(stats);
            }

            @Override
            public void onError(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
}

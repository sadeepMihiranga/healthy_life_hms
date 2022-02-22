package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface MedicineService {

    List<MedicineDTO> getAllMedicinesDropdown();

    @Transactional
    MedicineDTO insertMedicine(MedicineDTO medicineDTO);

    @Transactional
    MedicineDTO updateMedicine(Long medicineId, MedicineDTO medicineDTO);

    @Transactional
    Boolean removeMedicine(Long medicineId);

    PaginatedEntity medicinePaginatedSearch(String name, String brand, String type, Integer page, Integer size);

    MedicineDTO getMedicineById(Long medicineId);
}

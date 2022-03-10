package lk.healthylife.hms.service;

import lk.healthylife.hms.dto.MedicineDTO;
import lk.healthylife.hms.dto.PaginatedEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface MedicineService {

    List<MedicineDTO> getAllMedicinesDropdown();

    MedicineDTO insertMedicine(MedicineDTO medicineDTO);

    MedicineDTO updateMedicine(Long medicineId, MedicineDTO medicineDTO);

    Boolean removeMedicine(Long medicineId);

    PaginatedEntity medicinePaginatedSearch(String name, String brand, String type, Integer page, Integer size);

    MedicineDTO getMedicineById(Long medicineId);
}

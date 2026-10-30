package com.davinchicoder.employee_service;

import com.davinchicoder.employee_service.controller.EmployeeController;
import com.davinchicoder.employee_service.model.Employee;
import com.davinchicoder.employee_service.repository.EmployeeRepository;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

public abstract class ContractTestBase {
    @BeforeEach
    void setup() {
        EmployeeRepository repository = new EmployeeRepository();
        repository.create(Employee.builder().id(1L).departmentId(1L)
                .name("John Doe").position("Engineer").build());
        RestAssuredMockMvc.standaloneSetup(new EmployeeController(repository));
    }
}
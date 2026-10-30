package com.davinchicoder.department_service.IT;

import com.davinchicoder.department_service.client.EmployeeClient;
import com.davinchicoder.department_service.model.Department;
import com.davinchicoder.department_service.model.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.boot.test.web.client.TestRestTemplate;


import org.springframework.test.context.ActiveProfiles;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("local")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false"})
class DepartmentControllerIT {
    @Autowired
    private TestRestTemplate restTemplate;

    @TestBean(name = "com.davinchicoder.department_service.client.EmployeeClient", methodName = "employeeClient")
    private EmployeeClient employeeClient;

    static EmployeeClient employeeClient() {
        return id -> List.of(Employee.builder().id(1L).departmentId(id)
                .name("John Doe").position("Engineer").build());
    }

    @Test
    void createsAndReadsDepartmentWithEmployees() {
        var creation = restTemplate.postForEntity("/api/v1/departments",
                Department.builder().id(1L).name("Compras").build(), Void.class);
        assertEquals(200, creation.getStatusCode().value());
        Department department = restTemplate.getForObject("/api/v1/departments/1", Department.class);
        assertNotNull(department);
        assertEquals("Compras", department.getName());
        assertEquals(1, department.getEmployees().size());
        assertEquals("John Doe", department.getEmployees().get(0).getName());
    }
}
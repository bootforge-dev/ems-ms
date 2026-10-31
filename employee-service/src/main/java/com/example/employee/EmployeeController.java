package com.example.employee;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeRepository repository;
    private final RestClient departmentClient;

    public EmployeeController(EmployeeRepository repository, RestClient departmentClient) {
        this.repository = repository;
        this.departmentClient = departmentClient;
    }

    @GetMapping
    public List<Employee> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Employee> one(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Inter-service call: employee-service -> department-service
    @GetMapping("/{id}/details")
    public ResponseEntity<EmployeeDetails> details(@PathVariable Long id) {
        return repository.findById(id).map(emp -> {
            DepartmentDto dept = null;
            if (emp.getDepartmentId() != null) {
                try {
                    dept = departmentClient.get()
                            .uri("/api/departments/{id}", emp.getDepartmentId())
                            .retrieve()
                            .body(DepartmentDto.class);
                } catch (Exception ex) {
                    // department-service down or id not found: return employee without department
                }
            }
            return ResponseEntity.ok(new EmployeeDetails(emp, dept));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Employee create(@RequestBody Employee employee) {
        employee.setId(null);
        return repository.save(employee);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable Long id, @RequestBody Employee in) {
        return repository.findById(id).map(e -> {
            e.setName(in.getName());
            e.setEmail(in.getEmail());
            e.setDepartmentId(in.getDepartmentId());
            return ResponseEntity.ok(repository.save(e));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

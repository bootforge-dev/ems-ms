package com.example.department;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository repository;

    public DepartmentController(DepartmentRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Department> all() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Department> one(@PathVariable Long id) {
        return repository.findById(id).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department create(@RequestBody Department department) {
        department.setId(null);
        return repository.save(department);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Department> update(@PathVariable Long id, @RequestBody Department in) {
        return repository.findById(id).map(d -> {
            d.setName(in.getName());
            d.setLocation(in.getLocation());
            return ResponseEntity.ok(repository.save(d));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

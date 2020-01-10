package payroll;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

@RestController
@CrossOrigin
class ItemController {

	private final ItemRepository repository;

	ItemController(ItemRepository repository) {
		this.repository = repository;
	}

	// Aggregate root

	@GetMapping("v1/items")
	List<Item> all() {
		return repository.findAll();
	}

	@PostMapping("v1/items")
	Item newItem(@RequestBody Item newItem) {
		return repository.save(newItem);
	}

	// Single item
	
	@GetMapping("v1/items/{id}")
	Item one(@PathVariable Long id) {
		
		return repository.findById(id)
			.orElseThrow(() -> new ItemNotFoundException(id));
	}

	@PutMapping("v1/items/{id}")
	Item replaceItem(@RequestBody Item newItem, @PathVariable Long id) {
		
		return repository.findById(id)
			.map(item -> {
				item.setItem_name(newItem.getItem_name());
				item.setComplete(newItem.getComplete());
				return repository.save(item);
			})
			.orElseGet(() -> {
				newItem.setId(id);
				return repository.save(newItem);
			});
	}

	@DeleteMapping("v1/items/{id}")
	void deleteITem(@PathVariable Long id) {
		repository.deleteById(id);
	}
}

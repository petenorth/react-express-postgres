package payroll;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Data
@Entity
class Item {

	private @Id @GeneratedValue Long id;
	private String item_name;
	private String complete;

	Item() {}

	Item(String item_name, String complete) {
		this.item_name = item_name;
		this.complete = complete;
	}
}

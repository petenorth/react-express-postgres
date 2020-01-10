package payroll;

class ItemNotFoundException extends RuntimeException {

	ItemNotFoundException(Long id) {
		super("Could not find employee " + id);
	}
}

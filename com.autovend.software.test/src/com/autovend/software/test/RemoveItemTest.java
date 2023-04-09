package com.autovend.software.test;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.autovend.Barcode;
import com.autovend.BarcodedUnit;
import com.autovend.Numeral;
import com.autovend.PriceLookUpCode;
import com.autovend.PriceLookUpCodedUnit;
import com.autovend.devices.BarcodeScanner;
import com.autovend.devices.ElectronicScale;
import com.autovend.devices.TouchScreen;
import com.autovend.external.ProductDatabases;
import com.autovend.products.BarcodedProduct;
import com.autovend.products.PLUCodedProduct;
import com.autovend.software.controllers.BarcodeScannerController;
import com.autovend.software.controllers.CheckoutController;
import com.autovend.software.controllers.ElectronicScaleController;
import com.autovend.software.controllers.ItemRemoverController;

public class RemoveItemTest {
	
	private CheckoutController checkoutController;
	private BarcodeScannerController scannerController;
	private ElectronicScaleController scaleController;
	private ItemRemoverController removerController;
	private BarcodedProduct Item1;
	private PLUCodedProduct Item2;
	private BarcodedProduct Item3;
	private BarcodedUnit validUnit1;
	private PriceLookUpCodedUnit validUnit2;
	private BarcodedUnit validUnit3;

	BarcodeScanner stubScanner;
	ElectronicScale stubScale;
	TouchScreen stubRemover;

	/**
	 * Setup for testing
	 */
	@Before
	public void setup() {
		checkoutController = new CheckoutController();
		scannerController = new BarcodeScannerController(new BarcodeScanner());
		scaleController = new ElectronicScaleController(new ElectronicScale(1000, 1));
		removerController = new ItemRemoverController(new TouchScreen());

		// First item to be scanned
		Item1 = new BarcodedProduct(new Barcode(Numeral.three, Numeral.three), "test item 1",
				BigDecimal.valueOf(83.29), 359.0);

		// Second item to be added
		Item2 = new PLUCodedProduct(new PriceLookUpCode(Numeral.four, Numeral.zero, Numeral.one, Numeral.one), "test item 2",
				BigDecimal.valueOf(8.99));
		
		// Third item to be scanned
		Item3 = new BarcodedProduct(new Barcode(Numeral.four, Numeral.five), "test item 3", 
				BigDecimal.valueOf(42), 60.0);
		

		validUnit1 = new BarcodedUnit(new Barcode(Numeral.three, Numeral.three), 359.0);
		validUnit2 = new PriceLookUpCodedUnit(new PriceLookUpCode(Numeral.four, Numeral.zero, Numeral.one, Numeral.one), 60.0);
		validUnit3 = new BarcodedUnit(new Barcode(Numeral.one, Numeral.four),400.0);

		ProductDatabases.BARCODED_PRODUCT_DATABASE.put(Item1.getBarcode(), Item1);
		ProductDatabases.PLU_PRODUCT_DATABASE.put(Item2.getPLUCode(), Item2);
		ProductDatabases.BARCODED_PRODUCT_DATABASE.put(Item3.getBarcode(), Item3);

		stubScanner = new BarcodeScanner();
		stubScale = new ElectronicScale(1000, 1);
		stubRemover = new TouchScreen();

		scannerController = new BarcodeScannerController(stubScanner);
		scannerController.setMainController(checkoutController);
		scaleController = new ElectronicScaleController(stubScale);
		scaleController.setMainController(checkoutController);
		removerController = new ItemRemoverController(stubRemover);
		removerController.setMainController(checkoutController);

		stubScanner.register(scannerController);
		stubScale.register(scaleController);
		stubRemover.register(removerController);

	}
	
	/**
	 * Tears down objects so they can be initialized again with setup
	 */
	@After
	public void teardown() {
		checkoutController = null;
		scannerController = null;
		scaleController = null;
		removerController = null;
		stubScale = null;
		stubScanner = null;
		stubRemover = null;

	}
	
	@Test
	public void testNewScreen() {
		TouchScreen newScreen = new TouchScreen();
		removerController.setDevice(newScreen);
		assertNotSame("New screen should be ..", stubRemover, removerController.getDevice());
	}
	
	@Test
	public void testNewMainController() {
		CheckoutController newMainController = new CheckoutController();
		removerController.setMainController(newMainController);

		assertNotSame("New checkout controller should be set in ItemRemoverController field", checkoutController,
				removerController.getMainController());

	}

	@Test
	public void testRemove_invalidItemRemoverController() {

		checkoutController.removeItem(null, Item1, validUnit1.getWeight());

		assertEquals(0, checkoutController.getOrder().size());
		assertEquals(BigDecimal.ZERO, checkoutController.getCost());

	}
	
	@Test
	public void testRemoveItemNotInOrder() {
		
		
	    
		// Add an item to the cart, then try to remove a different item
	    checkoutController.addItem(scannerController, Item1, validUnit1.getWeight());
	    checkoutController.removeItem(removerController, Item2, validUnit2.getWeight());
	    assertEquals(BigDecimal.valueOf(83.29), checkoutController.getCost());
	}
	
	@Test
	public void testRemoveOnlyItemInOrder() {
		
	    checkoutController.addItem(scannerController, Item1, validUnit1.getWeight());
	    
	    assertTrue(checkoutController.getOrder().containsKey(Item1));

		checkoutController.removeItem(removerController, Item1, validUnit1.getWeight());

		assertEquals(0, checkoutController.getOrder().size());

	}
	
	@Test
	public void testRemoveItem_MultipleItemsInOrder() {
	    // Add two items to the cart, then remove one of them
	    checkoutController.addItem(scannerController, Item1, validUnit1.getWeight());
	    
		// Unblocks the station and lets a new item be scanned
		checkoutController.baggedItemsValid(scaleController);
	    
	    checkoutController.addItem(scannerController, Item3, validUnit3.getWeight());
	    
		// Unblocks the station and lets a new item be scanned
		checkoutController.baggedItemsValid(scaleController);
	    
	    checkoutController.removeItem(removerController, Item3, validUnit3.getWeight());
	    
	    assertEquals(1, checkoutController.getOrder().size());
	    assertEquals(BigDecimal.valueOf(83.29), checkoutController.getCost());
	    
	}
	
	@Test
	public void testRemoveItem_DuplicateItemsInOrder() {
	    
		// Add Item 1 to the order
	    checkoutController.addItem(scannerController, Item1, validUnit1.getWeight());
	    
		// Unblocks the station and lets a new item be scanned
		checkoutController.baggedItemsValid(scaleController);
	    
	    // Add Item 3 to the order
		checkoutController.addItem(scannerController, Item3, validUnit3.getWeight());
	    
		// Unblocks the station and lets a new item be scanned
		checkoutController.baggedItemsValid(scaleController);
		
		// Add duplicate Item 1 to the order again
	    checkoutController.addItem(scannerController, Item1, validUnit1.getWeight());
	    
		// Unblocks the station and lets a new item be scanned
		checkoutController.baggedItemsValid(scaleController);
		
	    checkoutController.removeItem(removerController, Item1, validUnit1.getWeight());
	    
	    assertEquals(2, checkoutController.getOrder().size());
	    assertEquals(BigDecimal.valueOf(125.29), checkoutController.getCost());
	    
	}
	
	@Test
	public void testDiscrepancyResolved() {
		scaleController.resetOrder();
		scaleController.attendantInput(true);
		scaleController.reactToWeightChangedEvent(stubScale, 10.0);
		assertFalse(scaleController.getMainController().baggingItemLock);
	}

	@Test
	public void testDiscrepancUnesolved() {
		scaleController.resetOrder();
		scaleController.attendantInput(false);
		scaleController.reactToWeightChangedEvent(stubScale, 10.0);
		assertTrue(scaleController.getMainController().baggingItemLock);
	}
	
	
	
	

}

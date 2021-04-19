import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.mysql.jdbc.Driver;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

class Main {

	private JFrame mainFrame;
	private JPanel panel1, panel2;
	private JTabbedPane tabPane;

	private JLabel lbl_name, lbl_ID, lbl_Name, lbl_ItemName, lbl_Qty, lbl_BillAmt;
	private JTextField txtField1_Name, txtField2_Name, txtField_Qty, txtField_BillAmt;

	private JComboBox<Integer> txtField_ID;
	private JComboBox<String> txtField_Item;
	private JButton savebtn, resetbtn, addbtn, proceedbtn;

	public static void main(String args[]) {
		Main pro = new Main();
		pro.displayAppGUI();
	}

	Main() {

		// Function calls to initialize all the GUI components and to populate the drop
		// down boxes.

		initializeAppGUI();

		populate_txtField_ID_And_txtField_Item();

		// ACTION LISTNER OF THE resetbtn - this should reset the txtField1

		resetbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				txtField1_Name.setText("");
			}
		});

		// ACTION LISTNER OF THE savebtn - this event automatically generates the
		// customer id using the getCustomerID() and inserts the customer id and name
		// into the CUSTOMER table.

		savebtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Connection con;
				PreparedStatement st;

				int customerid; // used to store the customer id
				String value1; // used to store the customer name

				try {
					con = getConnection();
					customerid = getCustomerID(con);
					value1 = txtField1_Name.getText();

					st = con.prepareStatement("insert into customer values(?,?)");
					st.setInt(1, customerid);
					st.setString(2, value1);

					st.executeUpdate();
					JOptionPane.showMessageDialog(panel1, "Data is successfully inserted into database.");

					txtField_ID.addItem(customerid);

				} catch (ClassNotFoundException e) {
					JOptionPane.showMessageDialog(panel1, "Error in submitting data!");
				} catch (SQLException ex) {
					Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});

		// ITEM LISTNER OF THE txtField_ID - this event is used to retrieve the customer
		// name from the CUSTOMER table, when the customer id is chosen from the drop
		// down box. It then displays name in the txtField2_Name text box.

		txtField_ID.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent ie) {

				Connection con;
				PreparedStatement st;
				ResultSet res;

				int value; // used to store the selected customer id
				try {
					con = getConnection();
					try {
						value = (Integer) txtField_ID.getSelectedItem();
					} catch (Exception exz) {
						txtField2_Name.setText("");
						return;
					}
					st = con.prepareStatement("select name from customer where cid = ?");
					st.setInt(1, value);
					res = st.executeQuery();
					if (res.next())
						txtField2_Name.setText(res.getString(1));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		/*
		 * ACTION LISTNER OF THE addbtn - The record from the ITEM table is retrieved
		 * for the item chosen from the drop down list. If required quantity is
		 * available in the ITEM table, a record is inserted into the PURCHASE table
		 * (customer id, item id, quantity, price) and the quantity field in the ITEM
		 * table is updated. Else, an error message is thrown.
		 **/
		addbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Connection con;
				PreparedStatement st;
				ResultSet res;

				String value1; // used to store selected item name
				Integer value2; // used to store the quantity

				int iid; // used to store the item id

				try {
					con = getConnection();
					value1 = (String) txtField_Item.getSelectedItem();
					if (value1.equals("Choose the Item")) {
						JOptionPane.showMessageDialog(panel2, "Please select an Item", "Item Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (txtField_ID.getSelectedItem() == null) {
						JOptionPane.showMessageDialog(panel2, "Please select a Customer", "Customer Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (txtField_Qty.getText().equals(""))
						JOptionPane.showMessageDialog(panel2, "Please enter a Quantity", "Quantity Not Entered",
								JOptionPane.ERROR_MESSAGE);
					else {
						try {
							value2 = Integer.parseInt(txtField_Qty.getText());
						} catch (Exception ex) {
							JOptionPane.showMessageDialog(panel2, "Quantity needs to be an integer.", "Quantity Error",
									JOptionPane.ERROR_MESSAGE);
							return;
						}
						st = con.prepareStatement("select qty,iid,price from item where name = ?");
						st.setString(1, value1);
						res = st.executeQuery();

						if (res.next()) {
							iid = res.getInt(2);
							if (value2 >= 0 && value2 <= res.getInt(1)) {
								// possible to buy
								st = con.prepareStatement("insert into purchase values(?,?,?,?)");
								st.setInt(1, (Integer) txtField_ID.getSelectedItem());
								st.setInt(2, iid);
								st.setInt(3, value2);
								st.setFloat(4, res.getFloat(3));

								st.executeUpdate();

								st = con.prepareStatement("update item set qty = ? where name = ?");
								st.setInt(1, (int) (res.getInt(1) - value2));
								st.setString(2, value1);
								st.executeUpdate();

								JOptionPane.showMessageDialog(panel2, "Item is successfully added to cart!");

								txtField_Qty.setText("");
								txtField_Item.setSelectedIndex(0);

							} else {
								// not possible
								JOptionPane.showMessageDialog(panel2, "Required Quantity not available.",
										"Quantity Not Available", JOptionPane.ERROR_MESSAGE);
								txtField_Qty.setText("");
							}
						}

					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

		/************************************************************************************************
		 * ACTION LISTNER OF THE proceedbtn - All records pertaining to a customer is
		 * retrieved from the PURCHASE table, the total bill amount is computed for all
		 * the items purchased by a customer.
		 ************************************************************************************************/
		proceedbtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				Connection con;
				PreparedStatement st;
				ResultSet res;

				int value; // used to store customer id
				float bill = 0; // used to store the bill amount
				try {
					con = getConnection();
					value = (Integer) txtField_ID.getSelectedItem();
					st = con.prepareStatement("select qty*price from purchase where cid = ?");
					st.setInt(1, value);
					res = st.executeQuery();
					while (res.next()) {
						bill += res.getFloat(1);
					}
					txtField_BillAmt.setText(Float.toString(bill));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		});

	}// end of constructor

	/************************************************************************************************
	 * TO POPULATE txtField_ID and txtField_Item from the CUSTOMER and ITEM tables.
	 ************************************************************************************************/
	public void populate_txtField_ID_And_txtField_Item() {
		Connection con;
		Statement statement1, statement2;
		ResultSet rs1, rs2;
		String query1, query2;

		try {
			con = getConnection();
			statement1 = con.createStatement();
			query1 = "select cid from customer";
			rs1 = statement1.executeQuery(query1);
			while (rs1.next()) {
				txtField_ID.addItem(rs1.getInt(1));
			}

			statement2 = con.createStatement();
			query2 = "select name from item";
			rs2 = statement2.executeQuery(query2);
			while (rs2.next()) {
				txtField_Item.addItem(rs2.getString(1));
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/************************************************************************************************
	 * TO INITIALIZE APP GUI
	 ************************************************************************************************/
	public void initializeAppGUI() {
		mainFrame = new JFrame("Online purchase");
		tabPane = new JTabbedPane();
		panel1 = new JPanel();
		panel2 = new JPanel();

		tabPane.addTab("New Customer", panel1);
		tabPane.addTab("Existing Customer", panel2);

		lbl_Name = new JLabel("Customer Name: ");
		txtField1_Name = new JTextField(10);
		savebtn = new JButton("Add");
		resetbtn = new JButton("Reset");

		lbl_ID = new JLabel("Customer ID: ");
		txtField_ID = new JComboBox<>();
		txtField_ID.addItem(null);
		txtField_ID.setSelectedIndex(0);
		lbl_name = new JLabel("Customer Name: ");
		txtField2_Name = new JTextField(10);
		lbl_ItemName = new JLabel("Item Name: ");
		txtField_Item = new JComboBox<String>();
		txtField_Item.addItem("Choose the Item");
		txtField_Item.setSelectedItem(0);
		lbl_Qty = new JLabel("Quantity: ");
		txtField_Qty = new JTextField(10);
		lbl_BillAmt = new JLabel("Bill Amount: ");
		txtField_BillAmt = new JTextField(10);
		addbtn = new JButton("Add More Items");
		proceedbtn = new JButton("Check out");

	}

	/************************************************************************************************
	 * TO DISPLAY APP GUI
	 ************************************************************************************************/
	void displayAppGUI() {

		panel1.setLayout(new FlowLayout());
		panel1.add(lbl_Name);
		panel1.add(txtField1_Name);
		panel1.add(savebtn);
		panel1.add(resetbtn);

		panel2.setLayout(new FlowLayout(FlowLayout.CENTER));
		panel2.add(lbl_ID);
		panel2.add(txtField_ID);
		panel2.add(lbl_name);
		panel2.add(txtField2_Name);
		panel2.add(lbl_ItemName);
		panel2.add(txtField_Item);
		panel2.add(lbl_Qty);
		panel2.add(txtField_Qty);
		panel2.add(lbl_BillAmt);
		panel2.add(txtField_BillAmt);
		panel2.add(addbtn);
		panel2.add(proceedbtn);

		mainFrame.getContentPane().add(tabPane);
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setVisible(true);
		mainFrame.setSize(400, 200);
		mainFrame.setResizable(true);

	}

	/************************************************************************************************
	 * TO GET DATABASE CONNECTION
	 ************************************************************************************************/
	public Connection getConnection() throws ClassNotFoundException, SQLException {
		Connection con;
		String url = "jdbc:mysql://localhost:3306/bms";
		String password = "rivu2001@#";
		String uname = "root";
		Class.forName("com.mysql.jdbc.Driver");
		con = DriverManager.getConnection(url, uname, password);
		return con;
	}

	/************************************************************************************************
	 * TO CREATE CUSTOMER, ITEM, AND PURCHASE TABLES TABLE : CUSTOMER, ATTRIBUTES -
	 * CID (int), NAME (var char) (Customer id and customer name) TABLE: ITEM,
	 * ATTRIBUTES - IID (int), NAME (var char), QTY (int), PRICE (float) (Item id,
	 * item name, quantity and price) TABLE: PURCHASE, ATTRIBUTES - CID (int), IID
	 * (int), QTY (int), PRICE (float) (Customer id, item id, quantity, and price)
	 ************************************************************************************************/
	public void createDatabase() throws ClassNotFoundException, SQLException {

		Connection con;
		Statement stmt;

		Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
		con = DriverManager.getConnection("jdbc:derby:cust;create=true;user=app;password=app");

		String createString = "create table customer( cid integer PRIMARY KEY, name varchar(30))";
		stmt = con.createStatement();
		stmt.executeUpdate(createString);

		createString = "create table item( iid integer, name varchar(30) PRIMARY KEY, qty integer, price float)";
		stmt = con.createStatement();
		stmt.executeUpdate(createString);

		createString = "create table purchase( cid integer , iid integer, qty integer, price float)";
		stmt = con.createStatement();
		stmt.executeUpdate(createString);

		// created 3 tables

	}

	/************************************************************************************************
	 * TO INSERT THREE ITEMS IN THE ITEM TABLE
	 ************************************************************************************************/
	public void insertItemInItemTable() {

		String query;
		Connection con;
		Statement stmt;

		try {
			con = getConnection();
			stmt = con.createStatement();
			query = "insert into item values(100,'Mobile',20,10000.50)";
			stmt.executeUpdate(query);

			query = "insert into item values(101,'Chocolates',15,100.50)";
			stmt.executeUpdate(query);

			query = "insert into item values(102,'Notebooks',10,50.50)";
			stmt.executeUpdate(query);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/******************************************************************************************************
	 * HELPER METHODS
	 ******************************************************************************************************/
	public int getCustomerID(Connection con) {

		int value = 0;
		ResultSet rs;
		Statement stmt;

		try {
			stmt = con.createStatement();
			rs = stmt.executeQuery("Select Max(CID) from customer");
			rs.next();
			if (rs.getInt(1) == 0)
				value = 100;
			else
				value = rs.getInt(1) + 1;
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return value;
	}

	public void deleteAllRecordsFromTable(String tableName) throws ClassNotFoundException, SQLException {
		String query;
		Connection con = getConnection();
		Statement stmt = con.createStatement();
		query = "DELETE FROM " + tableName;
		stmt.executeUpdate(query);
	}
}

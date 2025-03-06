package org.example;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class CampBaseGUI extends JFrame {
    private DBManager dbManager;
    private final CardLayout cardLayout;
    private final JPanel mainPanel;

    private String currentDbName;
    private String currentTableName;
    private String currentUsername;
    private String currentPassword;

    private JButton btnCreateDB;
    private JButton btnDropDB;
    private JButton btnCreateTable;
    private JButton btnClearTable;
    private JButton btnAddChild;
    private JButton btnUpdateChild;
    private JButton btnDeleteChild;

    private DefaultTableModel tableModel;

    public CampBaseGUI() {
        setTitle("Список детей на смене");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        JPanel loginPanel = createLoginPanel();
        JPanel operationsPanel = createOperationsPanel();

        mainPanel.add(loginPanel, "login");
        mainPanel.add(operationsPanel, "operations");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");

        setVisible(true);
    }

    private JPanel createLoginPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel lblDb = new JLabel("База данных:");
        JTextField tfDb = new JTextField("dataCamp", 15);

        JLabel lblRole = new JLabel("Роль:");
        JRadioButton rbAdmin = new JRadioButton("Admin");
        JRadioButton rbGuest = new JRadioButton("Guest");
        rbAdmin.setSelected(true);
        ButtonGroup bgRole = new ButtonGroup();
        bgRole.add(rbAdmin);
        bgRole.add(rbGuest);
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rolePanel.add(rbAdmin);
        rolePanel.add(rbGuest);

        JLabel lblPassword = new JLabel("Пароль:");
        JPasswordField pfPassword = new JPasswordField(15);
        JButton btnLogin = new JButton("Войти");

        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(lblDb, gbc);
        gbc.gridx = 1;
        panel.add(tfDb, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(lblRole, gbc);
        gbc.gridx = 1;
        panel.add(rolePanel, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(lblPassword, gbc);
        gbc.gridx = 1;
        panel.add(pfPassword, gbc);

        gbc.gridx = 1; gbc.gridy = 3;
        panel.add(btnLogin, gbc);

        btnLogin.addActionListener(e -> {
            String dbName = tfDb.getText().trim();
            String username = rbAdmin.isSelected() ? "admin" : "guest";
            String password = new String(pfPassword.getPassword());
            try {
                currentDbName = dbName;
                currentUsername = username;
                currentPassword = password;
                dbManager = new DBManager(dbName, username, password);
                dbManager.getConnection().close();
                dbManager.initProcedures();
                JOptionPane.showMessageDialog(this, "Подключение успешно");
                if (currentUsername.equalsIgnoreCase("guest")) {
                    updateOperationsPanelForGuest();
                }
                cardLayout.show(mainPanel, "operations");
                refreshChildrenTable("");
            } catch (SQLException ex) {
                if (ex.getMessage().contains("does not exist")) {
                    int response = JOptionPane.showConfirmDialog(
                            this,
                            "Database \"" + dbName + "\" does not exist. Create it automatically?",
                            "Database Not Found",
                            JOptionPane.YES_NO_OPTION
                    );
                    if (response == JOptionPane.YES_OPTION) {
                        try {
                            DBManager tempManager = new DBManager("postgres", username, password);
                            tempManager.createDatabase(dbName);
                            dbManager = new DBManager(dbName, username, password);
                            dbManager.getConnection().close();
                            dbManager.initProcedures();
                            currentDbName = dbName;
                            JOptionPane.showMessageDialog(this, "Database created successfully. Connection successful.");
                            if (currentUsername.equalsIgnoreCase("guest")) {
                                updateOperationsPanelForGuest();
                            }
                            cardLayout.show(mainPanel, "operations");
                            refreshChildrenTable("");
                        } catch (SQLException ex2) {
                            JOptionPane.showMessageDialog(this, "Error creating database: " + ex2.getMessage());
                        } catch (Exception ex2) {
                            JOptionPane.showMessageDialog(this, "Error initializing procedures: " + ex2.getMessage());
                        }
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Connection error: " + ex.getMessage());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error initializing procedures: " + ex.getMessage());
            }
        });

        return panel;
    }

    private void updateOperationsPanelForGuest() {
        if (btnCreateDB != null) btnCreateDB.setEnabled(false);
        if (btnDropDB != null) btnDropDB.setEnabled(false);
        if (btnCreateTable != null) btnCreateTable.setEnabled(false);
        if (btnClearTable != null) btnClearTable.setEnabled(false);
        if (btnAddChild != null) btnAddChild.setEnabled(false);
        if (btnUpdateChild != null) btnUpdateChild.setEnabled(false);
        if (btnDeleteChild != null) btnDeleteChild.setEnabled(false);
    }

    private void refreshChildrenTable(String surnameFilter) {
        try {
            List<Child> children = dbManager.searchChildBySurname(currentTableName, surnameFilter);
            tableModel.setRowCount(0);
            for (Child b : children) {
                tableModel.addRow(new Object[]{b.getId(), b.getSurname(), b.getName(), b.getLastName(), b.getGroup()});
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private JPanel createOperationsPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel dbPanel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout());

        JLabel lblNewDB = new JLabel("Имя базы:");
        JTextField tfNewDB = new JTextField(currentDbName != null ? currentDbName : "DataCamp", 10);
        JLabel lblNewTable = new JLabel("Имя таблицы:");
        JTextField tfNewTable = new JTextField("Children", 10);
        currentTableName = tfNewTable.getText().trim();

        btnCreateDB = new JButton("Создать БД");
        btnDropDB = new JButton("Удалить БД");
        btnCreateTable = new JButton("Создать таблицу");
        btnClearTable = new JButton("Очистить таблицу");

        if (currentUsername != null && currentUsername.equalsIgnoreCase("guest")) {
            btnCreateDB.setEnabled(false);
            btnDropDB.setEnabled(false);
            btnCreateTable.setEnabled(false);
            btnClearTable.setEnabled(false);
        }

        topPanel.add(lblNewDB);
        topPanel.add(tfNewDB);
        topPanel.add(btnCreateDB);
        topPanel.add(btnDropDB);
        topPanel.add(lblNewTable);
        topPanel.add(tfNewTable);
        topPanel.add(btnCreateTable);
        topPanel.add(btnClearTable);

        dbPanel.add(topPanel, BorderLayout.NORTH);

        btnCreateDB.addActionListener(e -> {
            try {
                String newDb = tfNewDB.getText().trim();
                dbManager.createDatabase(newDb);
                dbManager = new DBManager(newDb, currentUsername, currentPassword);
                dbManager.initProcedures();
                currentDbName = newDb;
                JOptionPane.showMessageDialog(this, "База данных создана и процедуры инициализированы");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error initializing procedures: " + ex.getMessage());
            }
        });

        btnDropDB.addActionListener(e -> {
            try {
                String newDb = tfNewDB.getText().trim();
                dbManager.dropDatabase(newDb);
                JOptionPane.showMessageDialog(this, "База данных удалена");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        btnCreateTable.addActionListener(e -> {
            try {
                String tableName = tfNewTable.getText().trim();
                currentTableName = tableName;
                dbManager.createTable(tableName);
                JOptionPane.showMessageDialog(this, "Таблица создана");
                refreshChildrenTable("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        btnClearTable.addActionListener(e -> {
            try {
                String tableName = tfNewTable.getText().trim();
                currentTableName = tableName;
                dbManager.clearTable(tableName);
                JOptionPane.showMessageDialog(this, "Таблица очищена");
                refreshChildrenTable("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        JPanel childPanel = new JPanel(new BorderLayout());
        JPanel topChildPanel = new JPanel(new FlowLayout());
        btnAddChild = new JButton("Добавить ребенка");
        btnUpdateChild = new JButton("Обновить данные о ребенке");
        btnDeleteChild = new JButton("Удалить ребенка по фамилии");
        JButton btnSearchChild = new JButton("Найти ребенка по фамилии");
        topChildPanel.add(btnAddChild);
        topChildPanel.add(btnUpdateChild);
        topChildPanel.add(btnDeleteChild);
        topChildPanel.add(btnSearchChild);

        JPanel inputPanel = new JPanel(new GridLayout(5, 2, 5, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Данные ребенка"));
        JTextField tfId = new JTextField();
        JTextField tfSurname = new JTextField();
        JTextField tfName = new JTextField();
        JTextField tfLastName = new JTextField();
        JTextField tfGroup = new JTextField();
        inputPanel.add(new JLabel("ID (для обновления):"));
        inputPanel.add(tfId);
        inputPanel.add(new JLabel("Surname:"));
        inputPanel.add(tfSurname);
        inputPanel.add(new JLabel("Name:"));
        inputPanel.add(tfName);
        inputPanel.add(new JLabel("Lastname:"));
        inputPanel.add(tfLastName);
        inputPanel.add(new JLabel("Group:"));
        inputPanel.add(tfGroup);

        tableModel = new DefaultTableModel(new Object[]{"ID", "surname", "name", "lastName", "group"}, 0);
        JTable resultTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(resultTable);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(inputPanel, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        childPanel.add(topChildPanel, BorderLayout.NORTH);
        childPanel.add(centerPanel, BorderLayout.CENTER);

        btnAddChild.addActionListener(e -> {
            try {
                String tableName = currentTableName;
                String surname = tfSurname.getText().trim();
                String name = tfName.getText().trim();
                String lastName = tfLastName.getText().trim();
                int group = Integer.parseInt(tfGroup.getText().trim());
                dbManager.addChild(tableName, surname, name, lastName, group);
                JOptionPane.showMessageDialog(this, "Ребенок добавлен");
                refreshChildrenTable("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid group format");
            }
        });

        btnSearchChild.addActionListener(e -> {
            String surname = tfSurname.getText().trim();
            refreshChildrenTable(surname);
        });

        btnUpdateChild.addActionListener(e -> {
            try {
                String tableName = currentTableName;
                int id = Integer.parseInt(tfId.getText().trim());
                String surname = tfSurname.getText().trim();
                String name = tfName.getText().trim();
                String lastName = tfLastName.getText().trim();
                int group = Integer.parseInt(tfGroup.getText().trim());
                dbManager.updateChild(tableName, id, surname, name, lastName, group);
                JOptionPane.showMessageDialog(this, "Данные о ребенке обновлены");
                refreshChildrenTable("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid numeric format");
            }
        });

        btnDeleteChild.addActionListener(e -> {
            try {
                String tableName = currentTableName;
                String surname = tfSurname.getText().trim();
                dbManager.deleteChildBySurname(tableName, surname);
                JOptionPane.showMessageDialog(this, "Ребенок удален");
                refreshChildrenTable("");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });

        tabbedPane.addTab("База данных", dbPanel);
        tabbedPane.addTab("Дети", childPanel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(tabbedPane, BorderLayout.CENTER);
        return panel;
    }
}
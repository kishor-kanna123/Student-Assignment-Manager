package com.studentmanager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Student Assignment Manager — compact runnable Swing/AWT + SQLite implementation.
 * The database is the source of truth; records persist between launches.
 */
public class Main {
    private static final Color NAVY = new Color(20, 33, 61);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final Color TEAL = new Color(13, 148, 136);
    private static final Color BG = new Color(245, 247, 251);
    private static final Color BORDER = new Color(226, 232, 240);
    private static final Color TEXT = new Color(23, 32, 51);
    private static final Path DB_PATH = Paths.get("data", "assignments.db");

    private JFrame frame;
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JComboBox<String> statusFilter;
    private JLabel totalValue, pendingValue, progressValue, overdueValue, pageTitle;
    private JPanel content;
    private CardLayout cardLayout;
    private final Map<String, JPanel> pages = new LinkedHashMap<>();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                initDatabase();
                new Main().showLogin();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Startup failed:\n" + ex.getMessage(),
                        "Student Assignment Manager", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static Connection connect() throws SQLException {
        try { Files.createDirectories(DB_PATH.getParent()); }
        catch (IOException e) { throw new SQLException("Cannot create data directory", e); }
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH.toAbsolutePath());
        try (Statement s = c.createStatement()) { s.execute("PRAGMA foreign_keys = ON"); }
        return c;
    }

    private static void initDatabase() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS assignments(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    description TEXT NOT NULL DEFAULT '',
                    due_date TEXT NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'Medium',
                    status TEXT NOT NULL DEFAULT 'Pending',
                    created_at TEXT NOT NULL
                )
                """);
            s.executeUpdate("CREATE INDEX IF NOT EXISTS idx_assignments_due ON assignments(due_date)");
        }
    }

    private void showLogin() {
        JFrame login = new JFrame("Student Assignment Manager");
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setSize(520, 470);
        login.setLocationRelativeTo(null);
        login.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG);
        JPanel hero = new JPanel();
        hero.setBackground(NAVY);
        hero.setBorder(new EmptyBorder(28, 28, 28, 28));
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        JLabel logo = new JLabel("✦  STUDYFLOW");
        logo.setForeground(Color.WHITE); logo.setFont(new Font("SansSerif", Font.BOLD, 15));
        JLabel headline = new JLabel("<html>Make every deadline<br>feel manageable.</html>");
        headline.setForeground(Color.WHITE); headline.setFont(new Font("SansSerif", Font.BOLD, 27));
        JLabel sub = new JLabel("Student Assignment Manager");
        sub.setForeground(new Color(203, 213, 225)); sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        hero.add(logo); hero.add(Box.createVerticalStrut(22)); hero.add(headline);
        hero.add(Box.createVerticalStrut(8)); hero.add(sub);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE); form.setBorder(new EmptyBorder(20, 28, 24, 28));
        GridBagConstraints g = new GridBagConstraints(); g.gridx=0; g.gridy=0; g.weightx=1;
        g.fill=GridBagConstraints.HORIZONTAL; g.insets=new Insets(5,0,5,0);
        JLabel label = new JLabel("Welcome to your workspace");
        label.setFont(new Font("SansSerif", Font.BOLD, 18)); label.setForeground(TEXT);
        form.add(label,g); g.gridy++;
        JLabel info = new JLabel("<html>This local demo opens directly into the assignment workspace.<br>Your records are saved on this computer.</html>");
        info.setForeground(new Color(100,116,139)); info.setFont(new Font("SansSerif",Font.PLAIN,12));
        form.add(info,g); g.gridy++;
        JButton start = primaryButton("Open dashboard");
        start.addActionListener(e -> { login.dispose(); showMainWindow(); });
        form.add(start,g); g.gridy++;
        JLabel note = new JLabel("Java Swing + AWT  •  SQLite persistence");
        note.setForeground(new Color(100,116,139)); note.setFont(new Font("SansSerif",Font.PLAIN,11));
        form.add(note,g);
        root.add(hero,BorderLayout.NORTH); root.add(form,BorderLayout.CENTER);
        login.setContentPane(root); login.setVisible(true);
    }

    private void showMainWindow() {
        frame = new JFrame("StudyFlow — Student Assignment Manager");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1050, 680));
        frame.setSize(1440, 900);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout());

        JPanel sidebar = new JPanel();
        sidebar.setBackground(NAVY); sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBorder(new EmptyBorder(22, 14, 18, 14));
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("✦  STUDYFLOW");
        brand.setForeground(Color.WHITE); brand.setFont(new Font("SansSerif", Font.BOLD, 17));
        brand.setBorder(new EmptyBorder(0, 8, 28, 0)); sidebar.add(brand);
        JLabel section = new JLabel("WORKSPACE");
        section.setForeground(new Color(148,163,184)); section.setFont(new Font("SansSerif",Font.BOLD,10));
        section.setBorder(new EmptyBorder(0,8,10,0)); sidebar.add(section);
        for (String name : java.util.List.of("Dashboard", "Assignments", "Reports")) {
            JButton nav = new JButton(navLabel(name));
            nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            nav.setHorizontalAlignment(SwingConstants.LEFT);
            nav.setFont(new Font("SansSerif", Font.PLAIN, 14));
            nav.setForeground(Color.WHITE); nav.setBackground(name.equals("Dashboard") ? new Color(37,54,86) : NAVY);
            nav.setBorder(new EmptyBorder(0, 13, 0, 8)); nav.setFocusPainted(false); nav.setOpaque(true);
            nav.addActionListener(e -> navigate(name));
            sidebar.add(nav); sidebar.add(Box.createVerticalStrut(5));
        }
        sidebar.add(Box.createVerticalGlue());
        JLabel local = new JLabel("<html><font color='#94A3B8'>LOCAL WORKSPACE</font><br><font color='#FFFFFF'>Your data stays on this PC</font></html>");
        local.setFont(new Font("SansSerif", Font.PLAIN, 11)); local.setBorder(new EmptyBorder(12,8,4,0)); sidebar.add(local);

        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setBackground(Color.WHITE); topbar.setBorder(BorderFactory.createMatteBorder(0,0,1,0,BORDER));
        topbar.setBorder(new EmptyBorder(18, 28, 18, 28));
        pageTitle = new JLabel("Dashboard");
        pageTitle.setFont(new Font("SansSerif", Font.BOLD, 22)); pageTitle.setForeground(TEXT);
        JLabel date = new JLabel(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy")));
        date.setForeground(new Color(100,116,139)); date.setFont(new Font("SansSerif",Font.PLAIN,12));
        topbar.add(pageTitle,BorderLayout.WEST); topbar.add(date,BorderLayout.EAST);

        cardLayout = new CardLayout(); content = new JPanel(cardLayout); content.setBackground(BG);
        pages.put("Dashboard", buildDashboard());
        pages.put("Assignments", buildAssignments());
        pages.put("Reports", buildReports());
        pages.forEach((key, panel) -> content.add(panel, key));
        main.add(topbar,BorderLayout.NORTH); main.add(content,BorderLayout.CENTER);
        frame.add(sidebar,BorderLayout.WEST); frame.add(main,BorderLayout.CENTER);
        refreshAll(); frame.setVisible(true);
    }

    private static String navLabel(String s) {
        return switch(s) { case "Dashboard" -> "▦   Dashboard"; case "Assignments" -> "☷   Assignments"; default -> "▤   Reports"; };
    }

    private JPanel buildDashboard() {
        JPanel p = new JPanel(new BorderLayout(0,18)); p.setBackground(BG);
        p.setBorder(new EmptyBorder(24,28,28,28));
        JPanel intro = new JPanel(new BorderLayout()); intro.setOpaque(false);
        JPanel copy = new JPanel(); copy.setOpaque(false); copy.setLayout(new BoxLayout(copy,BoxLayout.Y_AXIS));
        JLabel h = new JLabel("Good to see you 👋"); h.setFont(new Font("SansSerif",Font.BOLD,24)); h.setForeground(TEXT);
        JLabel sub = new JLabel("Here’s a snapshot of your academic workload.");
        sub.setFont(new Font("SansSerif",Font.PLAIN,13)); sub.setForeground(new Color(100,116,139));
        copy.add(h); copy.add(Box.createVerticalStrut(6)); copy.add(sub);
        JButton add = primaryButton("+  New assignment"); add.addActionListener(e -> showAssignmentDialog(null));
        intro.add(copy,BorderLayout.WEST); intro.add(add,BorderLayout.EAST);

        JPanel cards = new JPanel(new GridLayout(1,4,14,0)); cards.setOpaque(false);
        totalValue = new JLabel("0"); pendingValue = new JLabel("0"); progressValue = new JLabel("0"); overdueValue = new JLabel("0");
        cards.add(statCard("Total assignments",totalValue,"All your tasks",BLUE));
        cards.add(statCard("Pending",pendingValue,"Waiting to start",new Color(217,119,6)));
        cards.add(statCard("In progress",progressValue,"Currently working",TEAL));
        cards.add(statCard("Overdue",overdueValue,"Past deadline",new Color(220,38,38)));

        JPanel recent = cardPanel();
        recent.setLayout(new BorderLayout(0,12));
        JPanel rh = new JPanel(new BorderLayout()); rh.setOpaque(false);
        JLabel rt = new JLabel("Upcoming assignments"); rt.setFont(new Font("SansSerif",Font.BOLD,16)); rt.setForeground(TEXT);
        JButton view = textButton("View all →"); view.addActionListener(e -> navigate("Assignments"));
        rh.add(rt,BorderLayout.WEST); rh.add(view,BorderLayout.EAST);
        String[] cols={"Assignment","Subject","Due date","Priority","Status"};
        JTable upcoming = new JTable(new DefaultTableModel(cols,0) { public boolean isCellEditable(int r,int c){return false;} });
        styleTable(upcoming); upcoming.setRowHeight(38);
        JScrollPane sp = new JScrollPane(upcoming); sp.setBorder(BorderFactory.createLineBorder(BORDER)); sp.getViewport().setBackground(Color.WHITE);
        recent.add(rh,BorderLayout.NORTH); recent.add(sp,BorderLayout.CENTER);
        p.add(intro,BorderLayout.NORTH);
        JPanel middle = new JPanel(new BorderLayout(0,16)); middle.setOpaque(false);
        middle.add(cards,BorderLayout.NORTH); middle.add(recent,BorderLayout.CENTER);
        p.add(middle,BorderLayout.CENTER);
        p.putClientProperty("upcomingTable", upcoming);
        return p;
    }

    private JPanel buildAssignments() {
        JPanel p = new JPanel(new BorderLayout(0,16)); p.setBackground(BG); p.setBorder(new EmptyBorder(24,28,28,28));
        JPanel toolbar = new JPanel(new BorderLayout(10,10)); toolbar.setOpaque(false);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0)); left.setOpaque(false);
        searchField = new JTextField(20); searchField.setFont(new Font("SansSerif",Font.PLAIN,13));
        searchField.setPreferredSize(new Dimension(245,38)); searchField.putClientProperty("JTextField.placeholderText","Search assignments...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){
            public void insertUpdate(javax.swing.event.DocumentEvent e){applyFilters();}
            public void removeUpdate(javax.swing.event.DocumentEvent e){applyFilters();}
            public void changedUpdate(javax.swing.event.DocumentEvent e){applyFilters();}
        });
        statusFilter = new JComboBox<>(new String[]{"All statuses","Pending","In Progress","Submitted","Completed","Overdue"});
        statusFilter.setPreferredSize(new Dimension(155,38)); statusFilter.addActionListener(e -> applyFilters());
        left.add(searchField); left.add(Box.createHorizontalStrut(10)); left.add(statusFilter);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT,8,0)); actions.setOpaque(false);
        JButton export = secondaryButton("Export CSV"); export.addActionListener(e -> exportCsv());
        JButton edit = secondaryButton("Edit"); edit.addActionListener(e -> editSelected());
        JButton del = dangerButton("Delete"); del.addActionListener(e -> deleteSelected());
        JButton add = primaryButton("+ Add assignment"); add.addActionListener(e -> showAssignmentDialog(null));
        actions.add(export); actions.add(edit); actions.add(del); actions.add(add);
        toolbar.add(left,BorderLayout.WEST); toolbar.add(actions,BorderLayout.EAST);

        String[] cols={"ID","Assignment","Subject","Due date","Priority","Status"};
        tableModel = new DefaultTableModel(cols,0){ public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(tableModel); styleTable(table); table.setRowHeight(42); table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        sorter = new TableRowSorter<>(tableModel); table.setRowSorter(sorter);
        table.addMouseListener(new MouseAdapter(){ public void mouseClicked(MouseEvent e){if(e.getClickCount()==2) viewSelected();} });
        JPopupMenu menu = new JPopupMenu();
        JMenuItem viewItem = new JMenuItem("View details"); viewItem.addActionListener(e -> viewSelected());
        JMenuItem editItem = new JMenuItem("Edit"); editItem.addActionListener(e -> editSelected());
        JMenuItem doneItem = new JMenuItem("Mark completed"); doneItem.addActionListener(e -> updateSelectedStatus("Completed"));
        JMenuItem deleteItem = new JMenuItem("Delete"); deleteItem.addActionListener(e -> deleteSelected());
        menu.add(viewItem); menu.add(editItem); menu.add(doneItem); menu.addSeparator(); menu.add(deleteItem);
        table.setComponentPopupMenu(menu);
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createLineBorder(BORDER)); scroll.getViewport().setBackground(Color.WHITE);
        JPanel tableCard=cardPanel(); tableCard.setLayout(new BorderLayout()); tableCard.add(scroll,BorderLayout.CENTER);
        JLabel hint=new JLabel("  Double-click a row to view details. Select a row to edit, complete, or delete.");
        hint.setForeground(new Color(100,116,139)); hint.setFont(new Font("SansSerif",Font.PLAIN,11)); hint.setBorder(new EmptyBorder(10,0,0,0));
        p.add(toolbar,BorderLayout.NORTH); p.add(tableCard,BorderLayout.CENTER); p.add(hint,BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildReports() {
        JPanel p=new JPanel(new BorderLayout(0,16)); p.setBackground(BG); p.setBorder(new EmptyBorder(24,28,28,28));
        JLabel heading=new JLabel("Reports & exports"); heading.setFont(new Font("SansSerif",Font.BOLD,22)); heading.setForeground(TEXT);
        JLabel desc=new JLabel("Export your assignment records to a spreadsheet-compatible CSV file.");
        desc.setForeground(new Color(100,116,139)); desc.setFont(new Font("SansSerif",Font.PLAIN,13));
        JPanel intro=new JPanel(); intro.setOpaque(false); intro.setLayout(new BoxLayout(intro,BoxLayout.Y_AXIS)); intro.add(heading); intro.add(Box.createVerticalStrut(7)); intro.add(desc);
        JPanel card=cardPanel(); card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("Assignment report"); title.setFont(new Font("SansSerif",Font.BOLD,17)); title.setForeground(TEXT);
        JLabel text=new JLabel("<html>Includes assignment title, subject, deadline, priority, status, and description.<br>Choose a location and save a CSV file that opens in Excel or Google Sheets.</html>");
        text.setFont(new Font("SansSerif",Font.PLAIN,13)); text.setForeground(new Color(100,116,139));
        JButton export=primaryButton("Export all assignments"); export.setAlignmentX(Component.LEFT_ALIGNMENT); export.addActionListener(e -> exportCsv());
        card.add(title); card.add(Box.createVerticalStrut(12)); card.add(text); card.add(Box.createVerticalStrut(20)); card.add(export);
        p.add(intro,BorderLayout.NORTH); p.add(card,BorderLayout.CENTER); return p;
    }

    private JPanel statCard(String title, JLabel value, String subtitle, Color accent) {
        JPanel c=cardPanel(); c.setLayout(new BoxLayout(c,BoxLayout.Y_AXIS));
        JPanel row=new JPanel(new BorderLayout()); row.setOpaque(false);
        JLabel t=new JLabel(title); t.setForeground(new Color(100,116,139)); t.setFont(new Font("SansSerif",Font.PLAIN,12));
        JLabel dot=new JLabel("●"); dot.setForeground(accent); row.add(t,BorderLayout.WEST); row.add(dot,BorderLayout.EAST);
        value.setFont(new Font("SansSerif",Font.BOLD,30)); value.setForeground(TEXT);
        JLabel s=new JLabel(subtitle); s.setForeground(new Color(100,116,139)); s.setFont(new Font("SansSerif",Font.PLAIN,11));
        c.add(row); c.add(Box.createVerticalStrut(14)); c.add(value); c.add(Box.createVerticalStrut(5)); c.add(s);
        return c;
    }

    private JPanel cardPanel() {
        JPanel p=new JPanel(); p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(18,18,18,18)));
        return p;
    }

    private static JButton primaryButton(String text) {
        JButton b=new JButton(text); b.setBackground(BLUE); b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif",Font.BOLD,12)); b.setFocusPainted(false); b.setBorder(new EmptyBorder(11,16,11,16)); b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return b;
    }
    private static JButton secondaryButton(String text) {
        JButton b=new JButton(text); b.setBackground(Color.WHITE); b.setForeground(TEXT); b.setFont(new Font("SansSerif",Font.BOLD,12));
        b.setFocusPainted(false); b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER),new EmptyBorder(10,13,10,13)));
        b.setOpaque(true); return b;
    }
    private static JButton dangerButton(String text) {
        JButton b=secondaryButton(text); b.setForeground(new Color(220,38,38)); return b;
    }
    private static JButton textButton(String text) {
        JButton b=new JButton(text); b.setForeground(BLUE); b.setBackground(Color.WHITE); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setFont(new Font("SansSerif",Font.BOLD,12)); return b;
    }
    private static void styleTable(JTable t) {
        t.setFont(new Font("SansSerif",Font.PLAIN,12)); t.setForeground(TEXT); t.setBackground(Color.WHITE);
        t.setGridColor(new Color(241,245,249)); t.setShowVerticalLines(false); t.setIntercellSpacing(new Dimension(0,1));
        t.setSelectionBackground(new Color(219,234,254)); t.setSelectionForeground(TEXT); t.setFillsViewportHeight(true);
        JTableHeader h=t.getTableHeader(); h.setFont(new Font("SansSerif",Font.BOLD,12)); h.setForeground(new Color(71,85,105));
        h.setBackground(new Color(248,250,252)); h.setPreferredSize(new Dimension(h.getPreferredSize().width,40));
    }

    private void navigate(String page) {
        pageTitle.setText(page);
        cardLayout.show(content,page);
        if(page.equals("Assignments")) refreshAssignments();
        if(page.equals("Dashboard")) refreshDashboard();
    }

    private void refreshAll() { refreshAssignments(); refreshDashboard(); }

    private void refreshAssignments() {
        if(tableModel==null) return;
        tableModel.setRowCount(0);
        try(Connection c=connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT id,title,subject,due_date,priority,status FROM assignments ORDER BY due_date ASC")) {
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) tableModel.addRow(new Object[]{rs.getInt("id"),rs.getString("title"),rs.getString("subject"),
                        rs.getString("due_date"),rs.getString("priority"),displayStatus(rs.getString("status"),rs.getString("due_date"))});
            }
        } catch(SQLException ex) { showError("Could not load assignments",ex); }
        applyFilters();
    }

    private void refreshDashboard() {
        if(totalValue==null) return;
        int total=0,pending=0,progress=0,overdue=0;
        DefaultTableModel upcomingModel=null;
        JPanel dashboard=pages.get("Dashboard");
        JTable upcoming=(JTable)dashboard.getClientProperty("upcomingTable");
        if(upcomingModel==null && upcoming!=null) upcomingModel=(DefaultTableModel)upcoming.getModel();
        if(upcomingModel!=null) upcomingModel.setRowCount(0);
        try(Connection c=connect(); PreparedStatement ps=c.prepareStatement(
                "SELECT title,subject,due_date,priority,status FROM assignments ORDER BY due_date ASC")) {
            try(ResultSet rs=ps.executeQuery()) {
                while(rs.next()) {
                    total++;
                    String st=displayStatus(rs.getString("status"),rs.getString("due_date"));
                    if(st.equals("Pending")) pending++;
                    if(st.equals("In Progress")) progress++;
                    if(st.equals("Overdue")) overdue++;
                    if(upcomingModel!=null && upcomingModel.getRowCount()<7)
                        upcomingModel.addRow(new Object[]{rs.getString("title"),rs.getString("subject"),rs.getString("due_date"),rs.getString("priority"),st});
                }
            }
        } catch(SQLException ex) { showError("Could not load dashboard",ex); }
        totalValue.setText(String.valueOf(total)); pendingValue.setText(String.valueOf(pending));
        progressValue.setText(String.valueOf(progress)); overdueValue.setText(String.valueOf(overdue));
    }

    private static String displayStatus(String stored, String due) {
        if(!stored.equals("Completed") && !stored.equals("Submitted")) {
            try { if(LocalDate.parse(due).isBefore(LocalDate.now())) return "Overdue"; }
            catch(Exception ignored) {}
        }
        return stored;
    }

    private void applyFilters() {
        if(sorter==null) return;
        String query=searchField==null?"":searchField.getText().trim().toLowerCase(Locale.ROOT);
        String status=statusFilter==null?"All statuses":(String)statusFilter.getSelectedItem();
        java.util.List<RowFilter<Object,Object>> filters=new ArrayList<>();
        if(!query.isEmpty()) filters.add(new RowFilter<>() {
            public boolean include(Entry<?,?> e) {
                for(int i=1;i<e.getValueCount();i++) if(String.valueOf(e.getValue(i)).toLowerCase(Locale.ROOT).contains(query)) return true;
                return false;
            }
        });
        if(status!=null && !status.equals("All statuses")) filters.add(RowFilter.regexFilter("^"+java.util.regex.Pattern.quote(status)+"$",5));
        sorter.setRowFilter(filters.isEmpty()?null:RowFilter.andFilter(filters));
    }

    private void showAssignmentDialog(Integer id) {
        JTextField title=new JTextField(25), subject=new JTextField(25), due=new JTextField(LocalDate.now().plusDays(7).toString(),25);
        JTextArea description=new JTextArea(4,25); description.setLineWrap(true); description.setWrapStyleWord(true);
        JComboBox<String> priority=new JComboBox<>(new String[]{"Low","Medium","High","Urgent"});
        JComboBox<String> status=new JComboBox<>(new String[]{"Pending","In Progress","Submitted","Completed"});
        if(id!=null) {
            try(Connection c=connect(); PreparedStatement ps=c.prepareStatement("SELECT * FROM assignments WHERE id=?")) {
                ps.setInt(1,id);
                try(ResultSet rs=ps.executeQuery()) { if(rs.next()) {
                    title.setText(rs.getString("title")); subject.setText(rs.getString("subject"));
                    due.setText(rs.getString("due_date")); description.setText(rs.getString("description"));
                    priority.setSelectedItem(rs.getString("priority")); status.setSelectedItem(rs.getString("status"));
                }}
            } catch(SQLException ex) { showError("Could not load assignment",ex); return; }
        }
        JPanel form=new JPanel(new GridBagLayout()); form.setBorder(new EmptyBorder(8,8,8,8));
        GridBagConstraints g=new GridBagConstraints(); g.insets=new Insets(6,6,6,6); g.anchor=GridBagConstraints.WEST; g.fill=GridBagConstraints.HORIZONTAL;
        int y=0;
        addFormRow(form,g,y++,"Assignment title",title); addFormRow(form,g,y++,"Subject",subject);
        addFormRow(form,g,y++,"Due date (YYYY-MM-DD)",due); addFormRow(form,g,y++,"Priority",priority); addFormRow(form,g,y++,"Status",status);
        g.gridx=0;g.gridy=y;g.weightx=0; form.add(new JLabel("Description"),g);
        g.gridx=1;g.weightx=1;form.add(new JScrollPane(description),g);
        int result=JOptionPane.showConfirmDialog(frame,form,id==null?"Create assignment":"Edit assignment",
                JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
        if(result!=JOptionPane.OK_OPTION) return;
        String t=title.getText().trim(), s=subject.getText().trim(), d=due.getText().trim();
        if(t.isEmpty()||s.isEmpty()) { showWarning("Title and subject are required."); return; }
        try {
            LocalDate parsed=LocalDate.parse(d);
            if(parsed.isBefore(LocalDate.of(2000,1,1))) throw new IllegalArgumentException();
        } catch(Exception ex) { showWarning("Enter a valid due date in YYYY-MM-DD format."); return; }
        String pr=(String)priority.getSelectedItem(), st=(String)status.getSelectedItem();
        try(Connection c=connect()) {
            if(id==null) {
                try(PreparedStatement ps=c.prepareStatement("INSERT INTO assignments(title,subject,description,due_date,priority,status,created_at) VALUES(?,?,?,?,?,?,?)")) {
                    ps.setString(1,t);ps.setString(2,s);ps.setString(3,description.getText().trim());ps.setString(4,d);
                    ps.setString(5,pr);ps.setString(6,st);ps.setString(7,LocalDateTime.now().toString());ps.executeUpdate();
                }
            } else {
                try(PreparedStatement ps=c.prepareStatement("UPDATE assignments SET title=?,subject=?,description=?,due_date=?,priority=?,status=? WHERE id=?")) {
                    ps.setString(1,t);ps.setString(2,s);ps.setString(3,description.getText().trim());ps.setString(4,d);
                    ps.setString(5,pr);ps.setString(6,st);ps.setInt(7,id);ps.executeUpdate();
                }
            }
            refreshAll(); showSuccess(id==null?"Assignment created successfully.":"Assignment updated successfully.");
        } catch(SQLException ex) { showError("Could not save assignment",ex); }
    }

    private static void addFormRow(JPanel p,GridBagConstraints g,int y,String label,JComponent field) {
        g.gridy=y;g.gridx=0;g.weightx=0; p.add(new JLabel(label),g);
        g.gridx=1;g.weightx=1; p.add(field,g);
    }

    private Integer selectedId() {
        if(table==null) return null;
        int view=table.getSelectedRow(); if(view<0) return null;
        int model=table.convertRowIndexToModel(view);
        return ((Number)tableModel.getValueAt(model,0)).intValue();
    }
    private void editSelected() { Integer id=selectedId(); if(id==null){showWarning("Select an assignment first.");return;} showAssignmentDialog(id); }
    private void viewSelected() {
        Integer id=selectedId(); if(id==null){showWarning("Select an assignment first.");return;}
        try(Connection c=connect();PreparedStatement ps=c.prepareStatement("SELECT * FROM assignments WHERE id=?")) {
            ps.setInt(1,id);try(ResultSet rs=ps.executeQuery()){if(rs.next()){
                String msg="Title: "+rs.getString("title")+"\nSubject: "+rs.getString("subject")+"\nDue: "+rs.getString("due_date")+
                        "\nPriority: "+rs.getString("priority")+"\nStatus: "+displayStatus(rs.getString("status"),rs.getString("due_date"))+
                        "\nCreated: "+rs.getString("created_at")+"\n\nDescription:\n"+rs.getString("description");
                JOptionPane.showMessageDialog(frame,msg,"Assignment details",JOptionPane.INFORMATION_MESSAGE);
            }}
        } catch(SQLException ex){showError("Could not view assignment",ex);}
    }
    private void deleteSelected() {
        Integer id=selectedId(); if(id==null){showWarning("Select an assignment first.");return;}
        if(JOptionPane.showConfirmDialog(frame,"Delete this assignment permanently?","Confirm delete",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(Connection c=connect();PreparedStatement ps=c.prepareStatement("DELETE FROM assignments WHERE id=?")) {
            ps.setInt(1,id);ps.executeUpdate();refreshAll();showSuccess("Assignment deleted.");
        }catch(SQLException ex){showError("Could not delete assignment",ex);}
    }
    private void updateSelectedStatus(String status) {
        Integer id=selectedId(); if(id==null){showWarning("Select an assignment first.");return;}
        try(Connection c=connect();PreparedStatement ps=c.prepareStatement("UPDATE assignments SET status=? WHERE id=?")) {
            ps.setString(1,status);ps.setInt(2,id);ps.executeUpdate();refreshAll();showSuccess("Assignment marked "+status.toLowerCase()+".");
        }catch(SQLException ex){showError("Could not update assignment",ex);}
    }

    private void exportCsv() {
        JFileChooser chooser=new JFileChooser(); chooser.setDialogTitle("Export assignments to CSV");
        chooser.setSelectedFile(new File("assignments-report.csv"));
        if(chooser.showSaveDialog(frame)!=JFileChooser.APPROVE_OPTION)return;
        File out=chooser.getSelectedFile();
        if(!out.getName().toLowerCase(Locale.ROOT).endsWith(".csv")) out=new File(out.getParentFile(),out.getName()+".csv");
        try(Connection c=connect();PreparedStatement ps=c.prepareStatement("SELECT id,title,subject,description,due_date,priority,status,created_at FROM assignments ORDER BY due_date");
            ResultSet rs=ps.executeQuery();BufferedWriter w=Files.newBufferedWriter(out.toPath())) {
            w.write("ID,Title,Subject,Description,Due Date,Priority,Status,Created At");w.newLine();
            while(rs.next()) {
                for(int i=1;i<=8;i++){if(i>1)w.write(",");w.write(csv(rs.getString(i)));}
                w.newLine();
            }
            showSuccess("CSV exported to:\n"+out.getAbsolutePath());
        }catch(Exception ex){showError("Export failed",ex);}
    }
    private static String csv(String value) {
        if(value==null)return "";
        String safe=value.replace("\"","\"\"");
        return "\""+safe+"\"";
    }

    private void refreshDashboardSafely() { refreshDashboard(); }
    private void showError(String title,Exception ex) {
        JOptionPane.showMessageDialog(frame,title+":\n"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
    }
    private void showWarning(String message) { JOptionPane.showMessageDialog(frame,message,"Check input",JOptionPane.WARNING_MESSAGE); }
    private void showSuccess(String message) { JOptionPane.showMessageDialog(frame,message,"Success",JOptionPane.INFORMATION_MESSAGE); }
}

package vlsmCalculator;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VLSMCalculator extends JFrame {

    private static final long serialVersionUID = 1L;
    private JTextField ipInput;
    private JTextField hostInput;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private NetworkDiagramPanel diagramPanel;

    public VLSMCalculator() {
        setTitle("VLSM Calculator");
        setSize(1000, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Input Panel
        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        inputPanel.add(new JLabel("Enter Base IP (CIDR):"));
        ipInput = new JTextField("192.168.1.0/24");
        inputPanel.add(ipInput);

        inputPanel.add(new JLabel("Enter host requirements (comma separated):"));
        hostInput = new JTextField("100,50,25");
        inputPanel.add(hostInput);

        JButton calculateBtn = new JButton("Calculate VLSM");
        inputPanel.add(calculateBtn);
        add(inputPanel, BorderLayout.NORTH);

        // Table Setup
        String[] columnNames = {"Network", "Subnet Mask", "Hosts", "First IP", "Last IP"};
        tableModel = new DefaultTableModel(columnNames, 0);
        resultTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(resultTable);
        add(scrollPane, BorderLayout.CENTER);

        // Bottom panel with animation + credit label
        JPanel bottomPanel = new JPanel(new BorderLayout());
        diagramPanel = new NetworkDiagramPanel();
        JScrollPane diagramScroll = new JScrollPane(diagramPanel);
        diagramScroll.setPreferredSize(new Dimension(1000, 250));
        bottomPanel.add(diagramScroll, BorderLayout.CENTER);

        JLabel credit = new JLabel("Powered by @Jayson", SwingConstants.LEFT);
        credit.setFont(new Font("SansSerif", Font.BOLD, 14));
        credit.setForeground(Color.GRAY);
        bottomPanel.add(credit, BorderLayout.SOUTH);

        add(bottomPanel, BorderLayout.SOUTH);

        // Button Action
        calculateBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateVLSM();
            }
        });
    }

    private void calculateVLSM() {
        try {
            tableModel.setRowCount(0);
            diagramPanel.clearDevices();

            String ipCidr = ipInput.getText().trim();
            String[] parts = ipCidr.split("/");
            String baseIp = parts[0];
            String[] hostStrings = hostInput.getText().split(",");
            List<Integer> hosts = new ArrayList<>();
            for (String h : hostStrings) {
                hosts.add(Integer.parseInt(h.trim()));
            }

            hosts.sort(Collections.reverseOrder());

            long ip = ipToLong(InetAddress.getByName(baseIp));

            for (int i = 0; i < hosts.size(); i++) {
                int h = hosts.get(i);
                int needed = (int) Math.ceil(Math.log(h + 2) / Math.log(2));
                int subnetMask = 32 - needed;
                long blockSize = (long) Math.pow(2, needed);

                String subnetIp = longToIp(ip);
                String firstIp = longToIp(ip + 1);
                String lastIp = longToIp(ip + blockSize - 2);
                String mask = cidrToMask(subnetMask);

                Object[] row = {
                    subnetIp,
                    mask + "/" + subnetMask,
                    h,
                    firstIp,
                    lastIp
                };
                tableModel.addRow(row);
                diagramPanel.addSubnetDevice(i, subnetIp, h, firstIp, lastIp);

                ip += blockSize;
            }

            diagramPanel.repaint();

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }

    private long ipToLong(InetAddress ip) {
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result = result << 8 | (octet & 0xff);
        }
        return result;
    }

    private String longToIp(long ip) {
        return String.format("%d.%d.%d.%d",
            (ip >> 24) & 0xff,
            (ip >> 16) & 0xff,
            (ip >> 8) & 0xff,
            ip & 0xff);
    }

    private String cidrToMask(int cidr) {
        int mask = 0xffffffff << (32 - cidr);
        int octet1 = (mask >>> 24) & 0xff;
        int octet2 = (mask >>> 16) & 0xff;
        int octet3 = (mask >>> 8) & 0xff;
        int octet4 = mask & 0xff;
        return String.format("%d.%d.%d.%d", octet1, octet2, octet3, octet4);
    }

    class NetworkDiagramPanel extends JPanel {
        private final List<SubnetDevice> devices = new ArrayList<>

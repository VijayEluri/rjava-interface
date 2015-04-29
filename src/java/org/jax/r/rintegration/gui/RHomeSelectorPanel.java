/*
 * Copyright (c) 2009 The Jackson Laboratory
 * 
 * This software was developed by Gary Churchill's Lab at The Jackson
 * Laboratory (see http://research.jax.org/faculty/churchill).
 *
 * This is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jax.r.rintegration.gui;

import java.awt.Graphics;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.jax.r.rintegration.PlatformSpecificRFunctions;
import org.jax.r.rintegration.RInstallation;
import org.jax.r.rintegration.RInstallationScanner;
import org.jax.r.rintegration.RLaunchConfiguration;
import org.jax.r.rintegration.RLaunchConfiguration.LaunchUsingEnum;
import org.jax.r.rintegration.VersionStringComparator;
import org.jax.util.TextWrapper;
import org.jax.util.concurrent.SettableFuture;

/**
 * This panel was mostly generated by the netbeans visual editor 6.0, so be
 * careful to respect the comments when editing this class. It may be best
 * to make modifications using Netbeans.
 * The main purpose for this panel is to make it easy for the user to select
 * an R_HOME value.
 * @author <A HREF="mailto:keith.sheppard@jax.org">Keith Sheppard</A>
 */
public class RHomeSelectorPanel extends javax.swing.JPanel
{
    /**
     * our logger
     */
    private static final Logger LOG =
        Logger.getLogger(RHomeSelectorPanel.class.getName());
    
    /**
     * all {@link java.io.Serializable}s are supposed to have one of these
     */
    private static final long serialVersionUID = 6441170357710988036L;

    /**
     * the scanner that we use to detect R Homes
     */
    private final RInstallationScanner rHomeScanner = new RInstallationScanner();
    
    /**
     * the install dir structure to assume
     */
    private final PlatformSpecificRFunctions rPlatformSpecific;

    /**
     * the future R Home that lets us block callers
     */
    private volatile SettableFuture<RLaunchConfiguration> futureSelectedLaunchConfiguration =
        new SettableFuture<RLaunchConfiguration>();
    
    /**
     * the selected row number
     */
    private volatile int selectedRHomeRowNumber = -1;
    
    /**
     * Flag that tells us to warn the user (once) if we don't find any
     * R installations.
     */
    private volatile boolean warnAboutNoRInstallations;
    
    /**
     * Constructor
     * @param rPlatformSpecific
     *          see {@link PlatformSpecificRFunctions}
     * @param warnAboutNoRInstallations
     *          set this to true if the panel should warn the user
     *          when no R installations are automatically detected
     */
    // TODO remove inherit from environment stuff unless we're going to use it
    public RHomeSelectorPanel(
            PlatformSpecificRFunctions rPlatformSpecific,
            boolean warnAboutNoRInstallations)
    {
        this(null, rPlatformSpecific, warnAboutNoRInstallations);
    }

    /**
     * Constructor
     * @param initialInstallation
     *          the initial selection for R installation
     * @param rPlatformSpecific
     *          see {@link PlatformSpecificRFunctions}
     * @param warnAboutNoRInstallations
     *          set this to true if the panel should warn the user
     *          when no R installations are automatically detected
     */
    public RHomeSelectorPanel(
            RInstallation initialInstallation,
            PlatformSpecificRFunctions rPlatformSpecific,
            boolean warnAboutNoRInstallations)
    {
        this.rPlatformSpecific = rPlatformSpecific;
        this.warnAboutNoRInstallations = warnAboutNoRInstallations;
        
        // call the netbeans built init method
        this.initComponents();
        
        // call our init method
        this.postGuiInitialize(initialInstallation);
    }

    /**
     * This initialization method should be called after netbeans'
     * initialization method.
     * @param initialInstallation
     *          the installation to start off with
     */
    private void postGuiInitialize(RInstallation initialInstallation)
    {
        if(LOG.isLoggable(Level.FINE))
        {
            LOG.fine(
                    "Initializing RHomeSelectorPanel with initial installation: " +
                    initialInstallation);
        }
        
        // detect as many R_HOME's as you can
        RInstallation[] detectedRHomes = this.rHomeScanner.scanForRInstallations(
                  this.rPlatformSpecific);
        
        // populate the table with the R Homes that we found
        DefaultTableModel detectedRHomesTableModel = new DefaultTableModel()
        {
            /**
             * so we don't get serial id warnings
             */
            private static final long serialVersionUID = 5258785851158027018L;

            /**
             * {@inheritDoc}
             */
            @Override
            public boolean isCellEditable(int row, int column)
            {
                // no cellse are editable
                return false;
            }
        };
        detectedRHomesTableModel.setColumnIdentifiers(
                TableColumnEnum.values());
        for(int i = 0; i < detectedRHomes.length; i++)
        {
            // build a new table row
            RHomesTableCell[] currTableRow =
                new RHomesTableCell[TableColumnEnum.values().length];
            for(int j = 0; j < TableColumnEnum.values().length; j++)
            {
                // build a new table cell
                currTableRow[j] = new RHomesTableCell(
                        detectedRHomes[i],
                        TableColumnEnum.values()[j]);
            }
            
            // add the new row to the table model
            detectedRHomesTableModel.addRow(
                    currTableRow);
        }
        
        // toss our table model into the JTable
        this.detectedRHomesTable.setModel(detectedRHomesTableModel);
        
        this.detectedRHomesTable.setCellSelectionEnabled(false);
        this.detectedRHomesTable.setRowSelectionAllowed(true);
        ListSelectionModel selectionModel =
            this.detectedRHomesTable.getSelectionModel();
        selectionModel.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);
        if(initialInstallation != null)
        {
            for(int i = 0; i < detectedRHomes.length; i++)
            {
                if(detectedRHomes[i].equals(initialInstallation))
                {
                    selectionModel.setSelectionInterval(i, i);
                    break;
                }
            }
            
            this.setSelectedRHomeFile(initialInstallation.getRHomeDirectory());
        }
        
        // now take care of the row selection eventing
        selectionModel.addListSelectionListener(
                new ListSelectionListener()
                {
                    public void valueChanged(ListSelectionEvent e)
                    {
                        RHomeSelectorPanel.this.detectedRHomeSelectionChanged();
                    }
                });
        
        // resize the columns
        TableColumnModel columnModel =
            this.detectedRHomesTable.getColumnModel();
        for(int i = 0; i < columnModel.getColumnCount(); i++)
        {
            TableColumn currColumn = columnModel.getColumn(i);
            if(TableColumnEnum.values()[i] == TableColumnEnum.VERSION_COLUMN)
            {
                currColumn.setPreferredWidth(50);
            }
            else
            {
                currColumn.setPreferredWidth(400);
            }
        }
    }

    /**
     * Respond to a selection change in the {@link #detectedRHomesTable}.
     */
    private void detectedRHomeSelectionChanged()
    {
        // get the r home for the selected row
        int newlySelectedRowNumber = this.detectedRHomesTable.getSelectedRow();
        if(newlySelectedRowNumber != this.selectedRHomeRowNumber)
        {
            this.selectedRHomeRowNumber = newlySelectedRowNumber;
            if(newlySelectedRowNumber >= 0)
            {
                if(LOG.isLoggable(Level.FINEST))
                {
                    LOG.finest(
                            "newly selected R_HOME row is: " +
                            newlySelectedRowNumber);
                }
                RHomesTableCell selectedCell = (RHomesTableCell)this.detectedRHomesTable.getValueAt(
                        newlySelectedRowNumber,
                        0);
                this.setSelectedRHomeFile(
                        selectedCell.getRHome().getRHomeDirectory());
            }
            else
            {
                this.setSelectedRHomeFile(null);
            }
        }
    }

    /**
     * Apply the current settings.
     * @return
     *          true if the apply completes, false if the apply doesn't complete
     *          normally
     */
    public boolean apply()
    {
        // see if the user wants us to get the R_HOME from the environment
        // or not
        if(!this.getInheritRHomeFromEnvironment())
        {
            RInstallation selectedRHome =
                this.getValidatedSelectedRHomeNonBlocking();
            
            // don't do anything unless we have a non-null validated reference
            if(selectedRHome != null)
            {
                try
                {
                    if(!this.futureSelectedLaunchConfiguration.isDone())
                    {
                        this.futureSelectedLaunchConfiguration.set(new RLaunchConfiguration(
                                LaunchUsingEnum.LAUNCH_USING_SELECTED_INSTALLATION,
                                selectedRHome));
                    }
                }
                catch(Exception ex)
                {
                    LOG.log(Level.SEVERE,
                            "failed to set future R_HOME",
                            ex);
                }
                
                return true;
            }
            else
            {
                // validation failed (we got a null) so return false
                return false;
            }
        }
        else
        {
            try
            {
                if(!this.futureSelectedLaunchConfiguration.isDone())
                {
                    this.futureSelectedLaunchConfiguration.set(new RLaunchConfiguration(
                            LaunchUsingEnum.LAUNCH_USING_ENVIRONMENT,
                            null));
                }
            }
            catch(Exception ex)
            {
                LOG.log(Level.SEVERE,
                        "failed to set future R_HOME",
                        ex);
            }
            
            return true;
        }
    }

    /**
     * Cancel this selection.
     * @return
     *          true if cancel completes, false if the panel wants to stick
     *          around
     */
    public boolean cancel()
    {
        try
        {
            if(!this.futureSelectedLaunchConfiguration.isDone())
            {
                this.futureSelectedLaunchConfiguration.set(null);
            }
        }
        catch(Exception ex)
        {
            LOG.log(Level.SEVERE,
                    "failed to set future R_HOME to null",
                    ex);
        }
        
        return true;
    }
    
    /**
     * Reset the state of this R_HOME selector panel.
     */
    public void reset()
    {
        this.futureSelectedLaunchConfiguration =
            new SettableFuture<RLaunchConfiguration>();
        this.detectedRHomesTable.getSelectionModel().clearSelection();
        this.selectedRHomeTextField.setText("");
    }
    
    /**
     * Blocks until the user cancels or accepts an R launch configuration
     * @return
     *          the launch configuration or null if the user cancels
     */
    public RLaunchConfiguration getSelectedLaunchConfiguration()
    {
        try
        {
            return this.futureSelectedLaunchConfiguration.get();
        }
        catch(Exception ex)
        {
            LOG.log(Level.SEVERE,
                    "caught exception while blocking for selected " +
                    "launch configuration",
                    ex);
            return null;
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("all")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        inheritRHomeFromEnvironmentCheckBox = new javax.swing.JCheckBox();
        detectedRHomesLabel = new javax.swing.JLabel();
        detectedRHomesScrollPane = new javax.swing.JScrollPane();
        detectedRHomesTable = new javax.swing.JTable();
        selectedRHomeLabel = new javax.swing.JLabel();
        selectedRHomeTextField = new javax.swing.JTextField();
        browseRHomesButton = new javax.swing.JButton();

        inheritRHomeFromEnvironmentCheckBox.setText("Use Environment Variables (Not Recomended)");
        inheritRHomeFromEnvironmentCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                inheritRHomeFromEnvironmentCheckBoxItemStateChanged(evt);
            }
        });

        detectedRHomesLabel.setText("Detected R Installations:");

        detectedRHomesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        detectedRHomesScrollPane.setViewportView(detectedRHomesTable);

        selectedRHomeLabel.setText("R Home:");

        selectedRHomeTextField.addInputMethodListener(new java.awt.event.InputMethodListener() {
            public void inputMethodTextChanged(java.awt.event.InputMethodEvent evt) {
                selectedRHomeTextFieldInputMethodTextChanged(evt);
            }
            public void caretPositionChanged(java.awt.event.InputMethodEvent evt) {
            }
        });

        browseRHomesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/action/browse-16x16.png"))); // NOI18N
        browseRHomesButton.setText("Browse...");
        browseRHomesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseRHomesButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(detectedRHomesScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 513, Short.MAX_VALUE)
                    .add(layout.createSequentialGroup()
                        .add(selectedRHomeLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(selectedRHomeTextField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(browseRHomesButton))
                    .add(detectedRHomesLabel))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(detectedRHomesLabel)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(detectedRHomesScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 239, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(selectedRHomeLabel)
                    .add(browseRHomesButton)
                    .add(selectedRHomeTextField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Called when the user wants to browse the file system for the R_HOME
     * directory.
     * @param evt
     *          the button event
     */
    private void browseRHomesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseRHomesButtonActionPerformed
        JFileChooser rHomeFileChooser = new JFileChooser();
        rHomeFileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        rHomeFileChooser.setDialogTitle(
                "Select an R Home Directory");
        rHomeFileChooser.setAcceptAllFileFilterUsed(false);
        rHomeFileChooser.setApproveButtonMnemonic('S');
        int optionUserSelected = rHomeFileChooser.showDialog(
                this,
                "Select R Home");
        if(optionUserSelected == JFileChooser.APPROVE_OPTION)
        {
            this.setSelectedRHomeFile(
                    rHomeFileChooser.getSelectedFile());
        }
    }//GEN-LAST:event_browseRHomesButtonActionPerformed

    private void inheritRHomeFromEnvironmentCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_inheritRHomeFromEnvironmentCheckBoxItemStateChanged
        this.editsOccured();
        this.inheritRHomeFromEnvironmentChanged(
                evt.getStateChange() == ItemEvent.SELECTED);
    }//GEN-LAST:event_inheritRHomeFromEnvironmentCheckBoxItemStateChanged

    private void selectedRHomeTextFieldInputMethodTextChanged(java.awt.event.InputMethodEvent evt) {//GEN-FIRST:event_selectedRHomeTextFieldInputMethodTextChanged
        this.editsOccured();
    }//GEN-LAST:event_selectedRHomeTextFieldInputMethodTextChanged
    
    /**
     * this function is called when the "inherit from environment" state
     * changes
     * @param inheritRHomeFromEnvironment
     *          the new value
     */
    private void inheritRHomeFromEnvironmentChanged(
            boolean inheritRHomeFromEnvironment)
    {
        // enable or disable user selector controls
        this.detectedRHomesLabel.setEnabled(!inheritRHomeFromEnvironment);
        this.detectedRHomesTable.setEnabled(!inheritRHomeFromEnvironment);
        this.selectedRHomeLabel.setEnabled(!inheritRHomeFromEnvironment);
        this.selectedRHomeTextField.setEnabled(!inheritRHomeFromEnvironment);
        this.browseRHomesButton.setEnabled(!inheritRHomeFromEnvironment);
    }
    
    /**
     * Determines if the R_HOME and PATH come from the environment or if
     * they come from the selected {@link RInstallation}
     * <BR/>
     * <em>
     * WARNING: it is unsafe to call this function from any thread other than
     *          the AWT thread
     * </em>
     * @return
     *          true iff we're inheriting from our environment
     */
    private boolean getInheritRHomeFromEnvironment()
    {
        return this.inheritRHomeFromEnvironmentCheckBox.isSelected();
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseRHomesButton;
    private javax.swing.JLabel detectedRHomesLabel;
    private javax.swing.JScrollPane detectedRHomesScrollPane;
    private javax.swing.JTable detectedRHomesTable;
    private javax.swing.JCheckBox inheritRHomeFromEnvironmentCheckBox;
    private javax.swing.JLabel selectedRHomeLabel;
    private javax.swing.JTextField selectedRHomeTextField;
    // End of variables declaration//GEN-END:variables

    /**
     * Get the selected r home as a file (no validation is performed)
     * @return
     *          the selected R_HOME or null if there is no selected
     *          or entered R_HOME
     */
    private File getSelectedRHomeFile()
    {
        String selectedRHomeText =
            this.selectedRHomeTextField.getText().trim();
        if(selectedRHomeText.length() > 0)
        {
            return new File(
                    this.selectedRHomeTextField.getText());
        }
        else
        {
            return null;
        }
    }
    
    /**
     * Set the selected r home
     * @param selectedRHomeFile
     *          the new {@link RInstallation} selection
     */
    private void setSelectedRHomeFile(File selectedRHomeFile)
    {
        if(selectedRHomeFile == null)
        {
            this.selectedRHomeTextField.setText("");
        }
        else
        {
            this.selectedRHomeTextField.setText(
                    selectedRHomeFile.getAbsolutePath());
        }
    }
    
    /**
     * Get the user selected R Home.
     * <em>
     * WARNING: it is unsafe to call this function from any thread other than
     *          the AWT thread
     * </em>
     * @return
     *          the validated selected {@link RInstallation} or null if there is no
     *          selected {@link RInstallation} or validation fails
     */
    private RInstallation getValidatedSelectedRHomeNonBlocking()
    {
        RInstallation returnRHome = null;
        
        // 1st see if the user wants this to come from environment variables
        // before we even try to validate
        if(!this.getInheritRHomeFromEnvironment())
        {
            File selectedRHomeFile = this.getSelectedRHomeFile();
            if(selectedRHomeFile == null)
            {
                String message =
                    "Please either enter or select an R Home directory.";
                JOptionPane.showMessageDialog(
                        this,
                        TextWrapper.wrapText(
                                message,
                                TextWrapper.DEFAULT_DIALOG_COLUMN_COUNT),
                        "No R Home selected",
                        JOptionPane.ERROR_MESSAGE);
            }
            else
            {
                returnRHome = this.rHomeScanner.rHomeDirectoryToRInstallation(
                        this.rPlatformSpecific,
                        selectedRHomeFile);
                
                if(returnRHome == null)
                {
                    String errorMessage =
                        "Failed to detect an R installation at the R Home selected \"" +
                        selectedRHomeFile.getAbsolutePath() + "\".";
                    JOptionPane.showMessageDialog(
                            this,
                            TextWrapper.wrapText(
                                    errorMessage,
                                    TextWrapper.DEFAULT_DIALOG_COLUMN_COUNT),
                            "Invalid R Home",
                            JOptionPane.ERROR_MESSAGE);
                    if(LOG.isLoggable(Level.FINE))
                    {
                        LOG.fine("User error: " + errorMessage);
                    }
                }
                else
                {
                    String[] supportedSuperVersions =
                        this.rPlatformSpecific.getSupportedRSuperVersions();
                    VersionStringComparator versionComparator =
                        VersionStringComparator.getInstance();
                    if(supportedSuperVersions == null || supportedSuperVersions.length == 0)
                    {
                        String errorMessage =
                            "Internal error. No known supported R versions.";
                        JOptionPane.showMessageDialog(
                                this,
                                TextWrapper.wrapText(
                                        errorMessage,
                                        TextWrapper.DEFAULT_DIALOG_COLUMN_COUNT),
                                "Internal Error",
                                JOptionPane.ERROR_MESSAGE);
                        LOG.severe(
                                errorMessage + "(" + supportedSuperVersions +
                                ")");
                        returnRHome = null;
                    }
                    else
                    {
                        if(returnRHome.getRVersion() == null)
                        {
                            String errorMessage =
                                "Could not determine an R version for the selected " +
                                "R Home: " + returnRHome.getRHomeDirectory();
                            JOptionPane.showMessageDialog(
                                    this,
                                    TextWrapper.wrapText(
                                            errorMessage,
                                            TextWrapper.DEFAULT_DIALOG_COLUMN_COUNT),
                                    "Unknown R Version",
                                    JOptionPane.ERROR_MESSAGE);
                            LOG.warning(errorMessage);
                            returnRHome = null;
                        }
                        else
                        {
                            if(!versionComparator.areAnySuperVersionOf(
                                    supportedSuperVersions,
                                    returnRHome.getRVersion()))
                            {
                                StringBuffer errorMessage = new StringBuffer(
                                    "The selected R Home (version " +
                                    returnRHome.getRVersion() +
                                    ") does not appear to " +
                                    "to match any of the supported R versions: ");
                                for(int i = 0; i < supportedSuperVersions.length; i++)
                                {
                                    String currSupportedVersion =
                                        supportedSuperVersions[i];
                                    errorMessage.append(currSupportedVersion);
                                    errorMessage.append(".*");
                                    if(i < supportedSuperVersions.length - 1)
                                    {
                                        // if it isn't the last one, add a comma
                                        errorMessage.append(", ");
                                    }
                                }
                                JOptionPane.showMessageDialog(
                                        this,
                                        TextWrapper.wrapText(
                                                errorMessage.toString(),
                                                TextWrapper.DEFAULT_DIALOG_COLUMN_COUNT),
                                        "Unsupported R Version",
                                        JOptionPane.ERROR_MESSAGE);
                                LOG.warning(
                                        "User error: " + errorMessage);
                                returnRHome = null;
                            }
                        }
                    }
                }
            }
        }
        
        return returnRHome;
    }
    
    /**
     * The table column enum. Columns are declared in the order that they
     * should appear in the table.
     */
    private static enum TableColumnEnum
    {
        /**
         * the version column
         */
        VERSION_COLUMN
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString()
            {
                return "Version";
            }
        },
        
        /**
         * the r home column
         */
        R_HOME_COLUMN
        {
            /**
             * {@inheritDoc}
             */
            @Override
            public String toString()
            {
                return "R Home Directory";
            }
        }
    }
    
    /**
     * The type to throw into our R_HOME table's cells
     */
    private static class RHomesTableCell
    {
        /**
         * @see #getTableColumn()
         */
        private final TableColumnEnum tableColumn;
        
        /**
         * @see #getRHome()
         */
        private final RInstallation rHome;
        
        /**
         * Constructor
         * @param rHome
         *          see {@link #getRHome()}
         * @param tableColumn
         *          see {@link #getTableColumn()}
         */
        public RHomesTableCell(RInstallation rHome, TableColumnEnum tableColumn)
        {
            this.rHome = rHome;
            this.tableColumn = tableColumn;
        }

        /**
         * Get the table column for this cell.
         * @return the table column
         */
        public TableColumnEnum getTableColumn()
        {
            return this.tableColumn;
        }

        /**
         * Get the R Home for this cell
         * @return the rHome
         */
        public RInstallation getRHome()
        {
            return this.rHome;
        }
        
        /**
         * gets a string representation of this cell. the string will be
         * different depending on which column we're in
         */
        @Override
        public String toString()
        {
            // return a different string depending on which column we're at
            switch(this.tableColumn)
            {
                case R_HOME_COLUMN:
                {
                    return this.rHome.getRHomeDirectory().getAbsolutePath();
                }
                case VERSION_COLUMN:
                {
                    String rVersion = this.rHome.getRVersion();
                    if(rVersion == null)
                    {
                        // use a question mark if we don't know the version
                        return "?";
                    }
                    else
                    {
                        return rVersion;
                    }
                }
                default:
                {
                    // this is bad... we missed an enum
                    LOG.severe(
                            "Failed to have a case for R Home table column: " +
                            this.tableColumn.toString());
                    return "?";
                }
            }
        }
    }
    
    /**
     * deal with edit events
     */
    private void editsOccured()
    {
        // if the future user selections are already set, then we should
        // start over with a clean slate
        if(this.futureSelectedLaunchConfiguration.isDone())
        {
            this.futureSelectedLaunchConfiguration =
                new SettableFuture<RLaunchConfiguration>();
        }
    }

    /**
     * Paint method overridden with code to warn the user if we need to.
     * @param g
     *          the graphics context
     */
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);

        // see if we need to warn the user about not being able to detect
        // directories
        if(this.detectedRHomesTable.getRowCount() == 0 &&
           this.warnAboutNoRInstallations)
        {
            // warn the user that we cant find any R installations
            final StringBuilder message = new StringBuilder();
            message.append("Could not find any R installations in the default ");
            message.append("installation directory(s): ");
            File[] expectedInstallRoots = this.rPlatformSpecific.getExpectedInstallRoots();
            for(int i = 0; i < expectedInstallRoots.length; i++) {
                if(i >= 1) {
                    message.append(", ");
                }
                message.append(expectedInstallRoots[i]);
            }
            message.append(". If you know the location of an existing installation ");
            message.append("you can use the browse button to locate it. Otherwise, ");
            message.append("you should cancel and restart the application after ");
            message.append("installing R.");
            
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    JOptionPane.showMessageDialog(
                            RHomeSelectorPanel.this,
                            TextWrapper.wrapText(
                                    message.toString(),
                                    TextWrapper.DEFAULT_DIALOG_COLUMN_COUNT),
                            "No R Installations Detected",
                            JOptionPane.WARNING_MESSAGE);
                }
            });
            
            this.warnAboutNoRInstallations = false;
        }
    }
}

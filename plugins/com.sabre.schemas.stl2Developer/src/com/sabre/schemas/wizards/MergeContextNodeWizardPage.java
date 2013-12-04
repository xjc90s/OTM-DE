/*
 * Copyright (c) 2011, Sabre Inc.
 */
package com.sabre.schemas.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sabre.schemas.node.ContextNode;
import com.sabre.schemas.widgets.WidgetFactory;

/**
 * @author Agnieszka Janowska
 * 
 */
public class MergeContextNodeWizardPage extends WizardPage {

    private static final Logger LOGGER = LoggerFactory.getLogger(MergeContextNodeWizardPage.class);

    private Text applicationText;
    private Combo idCombo;
    private Text descriptionText;
    private Text warningsText;
    // private final TLLibrary library;
    private final ContextNode context;
    private ContextNode selected;
    private final FormValidator validator;

    protected MergeContextNodeWizardPage(final String pageName, final String title,
            ContextNode context, final FormValidator validator) {
        super(pageName, title, null);
        this.validator = validator;
        this.context = context;
        if (context == null) {
            LOGGER.error("NULL context.", this.getClass());
        }

    }

    @Override
    public void createControl(final Composite parent) {
        final GridLayout layout = new GridLayout();
        layout.numColumns = 2;

        final Composite container = new Composite(parent, SWT.BORDER);// parent;
        container.setLayout(layout);

        final GridData singleColumnGD = new GridData();
        singleColumnGD.horizontalSpan = 1;
        singleColumnGD.horizontalAlignment = SWT.FILL;
        singleColumnGD.grabExcessHorizontalSpace = true;

        final GridData multiTextGD = new GridData();
        multiTextGD.horizontalSpan = 2;
        multiTextGD.horizontalAlignment = SWT.FILL;
        multiTextGD.verticalAlignment = SWT.FILL;
        multiTextGD.grabExcessHorizontalSpace = true;
        multiTextGD.grabExcessVerticalSpace = true;

        final GridData introTextGD = new GridData();
        introTextGD.horizontalSpan = 2;
        introTextGD.horizontalAlignment = SWT.FILL;
        introTextGD.grabExcessHorizontalSpace = true;

        final Label introLabel = new Label(container, SWT.NONE);
        introLabel.setText("Merge " + context.getContextId() + " into selected context. ");
        introLabel.setLayoutData(introTextGD);
        final Label introLabel2 = new Label(container, SWT.NONE);
        introLabel2.setText("Reassigns the examples, equivalents, custom facets and query facets"
                + " contexts to the selected context.");
        introLabel2.setLayoutData(introTextGD);

        final Label idLabel = new Label(container, SWT.NONE);
        idLabel.setText("Application context:");
        idCombo = WidgetFactory.createCombo(container, SWT.DROP_DOWN | SWT.V_SCROLL | SWT.READ_ONLY);
        // Fill the combo drop down with the other contexts.
        for (final ContextNode ctx : context.getParent().getChildren()) {
            if (!ctx.getContextId().equals(context.getContextId())) {
                idCombo.add(ctx.getContextId());
                idCombo.setData(ctx.getContextId(), ctx);
            }
        }

        // for (final ContextNode ctx : context.getParent().getChildren()) {
        // if (!ctx.getContextId().equals(context.getContextId())) {
        // idCombo.add(ctx.getApplicationContext());
        // idCombo.setData(ctx.getApplicationContext(), ctx);
        // }
        // }
        // Set up the first node as the selected node.
        ContextNode initialCtx = context.getParent().getChildren().get(0);
        idCombo.select(0);
        selected = context.getParent().getChildren().get(0);
        idCombo.setLayoutData(singleColumnGD);
        idCombo.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(final ModifyEvent e) {
                final String applicationContext = idCombo.getText();
                selected = (ContextNode) idCombo.getData();
                updateView(applicationContext);
                validate();
            }
        });

        final Label applicationLabel = new Label(container, SWT.NONE);
        applicationLabel.setText("Context ID:");
        applicationText = WidgetFactory.createText(container, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY);
        applicationText.setLayoutData(singleColumnGD);

        final Label descriptionLabel = new Label(container, SWT.NONE);
        descriptionLabel.setText("Description:");
        descriptionText = WidgetFactory.createText(container, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
        descriptionText.setLayoutData(multiTextGD);

        final Label warningsLabel = new Label(container, SWT.NONE);
        warningsLabel.setText("Warnings:");
        warningsText = WidgetFactory.createText(container, SWT.MULTI | SWT.BORDER | SWT.READ_ONLY);
        warningsText.setLayoutData(multiTextGD);

        updateView(initialCtx.getApplicationContext());
        setControl(container);
        setPageComplete(true);
    }

    private void validate() {
        boolean complete = true;
        String message = null;
        int level = INFORMATION;

        try {
            validator.validate();
        } catch (final ValidationException e) {
            message = e.getMessage();
            level = ERROR;
            complete = false;
            LOGGER.debug("Validation output " + e.getMessage());
        }
        setPageComplete(complete);
        setMessage(message, level);
        if (message == null)
            warningsText.setText("All OK");
        else
            warningsText.setText(message);
        getWizard().getContainer().updateButtons();
    }

    /**
     * Update the ID display based on combo key
     * 
     * @param appCtx
     */
    private void updateView(String appCtx) {
        Object object = idCombo.getData(appCtx);
        if (object instanceof ContextNode) {
            ContextNode context = (ContextNode) object;
            selected = context;
            applicationText.setText(context.getApplicationContext());
            descriptionText.setText(context.getDescription());
        }
    }

    public ContextNode getContext() {
        return selected;
    }

}

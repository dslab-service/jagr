
/*
 * Copyright (c) 1998-2002 by Sun Microsystems, Inc. All Rights Reserved.
 * This software is the proprietary information of Sun Microsystems, Inc.
 * Use is subject to license terms.
 *
 * $Id: WorkOrderStateConstants.java,v 1.1 2004/02/19 14:45:05 emrek Exp $
 *
 */
package com.sun.ecperf.mfg.helper;


/**
 * Interface WorkOrderStateConstants
 *
 *
 * @author
 * @version %I%, %G%
 */
public interface WorkOrderStateConstants {

    public static final String[] woStates  = {
        "Open", "Stage1", "Stage2", "Stage3", "Completed", "Archived",
        "Cancelled"
    };
    public static final int      OPEN      = 0;
    public static final int      STAGE1    = 1;
    public static final int      STAGE2    = 2;
    public static final int      STAGE3    = 3;
    public static final int      COMPLETED = 4;
    public static final int      ARCHIVED  = 5;
    public static final int      CANCELLED = 6;
}


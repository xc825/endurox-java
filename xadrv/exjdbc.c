/**
 * @brief Common java XA driver loader
 *  Caches shall be loaded once we attempt to access any of the AtmiCtx
 *  thus init shall do that.
 *
 * @file exjdbc.c
 */
/* -----------------------------------------------------------------------------
 * Enduro/X Middleware Platform for Distributed Transaction Processing
 * Copyright (C) 2009-2016, ATR Baltic, Ltd. All Rights Reserved.
 * Copyright (C) 2017-2018, Mavimax, Ltd. All Rights Reserved.
 * This software is released under one of the following licenses:
 * AGPL or Mavimax's license for commercial use.
 * -----------------------------------------------------------------------------
 * AGPL license:
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License, version 3 as published
 * by the Free Software Foundation;
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License, version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * -----------------------------------------------------------------------------
 * A commercial use license is available from Mavimax, Ltd
 * contact@mavimax.com
 * -----------------------------------------------------------------------------
 */
#include <string.h>
#include <stdio.h>
#include <stdlib.h>

#include <ndrstandard.h>
#include <ndebug.h>
#include <atmi.h>
#include <sys_mqueue.h>

#include <tmenv.h>
#include <libsrc.h>

#include <xa.h>
#define __USE_GNU
#include <dlfcn.h>

#include "exjdbc.h"
/*---------------------------Externs------------------------------------*/
/*---------------------------Macros-------------------------------------*/
/*---------------------------Enums--------------------------------------*/
/*---------------------------Typedefs-----------------------------------*/
/*---------------------------Globals------------------------------------*/
/*---------------------------Statics------------------------------------*/
/*---------------------------Prototypes---------------------------------*/

/*
 * The function is exported and dynamically retrieved after the module was
 * fetched
 */
struct xa_switch_t *ndrx_get_xa_switch(void)
{
    struct xa_switch_t * sw = NULL;
    void *handle = NULL;
    int ret = EXSUCCEED;
    char *symbol = "ndrxjsw";
    char *descr = "Enduro/X JDBC";
    char *rmlib = getenv(CONF_NDRX_XA_RMLIB);
    ndrx_ctx_priv_t *ctxpriv = ndrx_ctx_priv_get();
    ndrx_env_priv_t *envpriv = ndrx_env_priv_get();
    
    NDRX_LOG(log_debug, "ex java rmlib = [%s]", descr, rmlib);
    
    sw = (struct xa_switch_t * )dlsym( RTLD_DEFAULT, symbol );
    if( sw == NULL )
    {
        NDRX_LOG(log_debug, "%s symbol not found in "
                "process address space - loading .so!", symbol);
        
        /* Loading the symbol... */
        handle = dlopen (rmlib, RTLD_NOW);
        if (!handle)
        {
            NDRX_LOG(log_error, "Failed to load XA Resource Manager lib [%s]: %s", 
                rmlib, dlerror());
            EXFAIL_OUT(ret);
        }
        
        /* resolve symbol now... */
        if (NULL==(sw = (struct xa_switch_t * )dlsym( handle, symbol )))
        {
            NDRX_LOG(log_error, "Oracle XA switch `%s' handler "
                    "not found!", symbol);
            EXFAIL_OUT(ret);
        }
        
        /* Check private data, if java env and context is not set
         * then we need to load these.
         * 
         * Also we need to mark in Context in Context in what mode we are started
         * so that in case of tpclose we can destroy the the Atmi Context.
         * What about java env?
         * Shall we leave it open?
         * Also these settings shall be stored in common/env storage
         * and not in thread local storage.
         * 
         * 1. OK, so if context is NOT set, then we operate not from C process
         * 2. If Context is set, then we operate from java process.
         */
        
    }
    
    /* Check are we C process (tmsrv) or java proc */
    if (NULL==NDRXJ_JATMICTX(ctxpriv))
    {
        NDRX_LOG(log_info, "This is TMSRV process");
        
        /* TODO: boot java env, store it in global env
         * then at tpopen we transfer this env to tls
         * at tpclose() we shall clear the TLS.
         * Load the JNI glue libs...
         */
        
        /* create java env... */
        if (EXSUCCEED!=ndrxj_jvm_create(ctxpriv))
        {
            NDRX_LOG(log_error, "Failed to create java env for TMSRV!");
            EXFAIL_OUT(ret);
        }
    }
    else
    {
        NDRX_LOG(log_info, "This is JAVA process");
    }
    
    if (NULL!=sw)
    {
        
        /* get the init function... 
         * we have different ones, for java processes & for tmsrv processes.
         * Call this func dynamically..
         */
        if (EXSUCCEED!=ndrxj_xa_init())
        {
            NDRX_LOG(log_error, "Failed to init JDBC driver");
            sw = NULL;
            EXFAIL_OUT(ret);
        }
        
        /* No need for tran suspend in contexting */
        ndrx_xa_noapisusp(EXTRUE);
    }
    
out:
    if (EXSUCCEED!=ret && NULL!=handle)
    {
        /* close the handle */
        dlclose(handle);
    }

    return sw;
}



#undef __USE_GNU
/* vim: set ts=4 sw=4 et smartindent: */

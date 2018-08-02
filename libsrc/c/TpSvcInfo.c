/**
 * @brief Support routines for TpSvcInfo class - translate C
 *   TPSVCINFO to TpSvcInfo
 *
 * @file TpSvcInfo.c
 */
/* -----------------------------------------------------------------------------
 * Enduro/X Middleware Platform for Distributed Transaction Processing
 * Copyright (C) 2009-2016, ATR Baltic, Ltd. All Rights Reserved.
 * Copyright (C) 2017-2018, Mavimax, Ltd. All Rights Reserved.
 * This software is released under one of the following licenses:
 * GPL or Mavimax's license for commercial use.
 * -----------------------------------------------------------------------------
 * GPL license:
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * -----------------------------------------------------------------------------
 * A commercial use license is available from Mavimax, Ltd
 * contact@mavimax.com
 * -----------------------------------------------------------------------------
 */

/*---------------------------Includes-----------------------------------*/
#include <jni.h>
#include <errno.h>
#include <stdlib.h>
#include "org_endurox_AtmiCtx.h"
#include <atmi.h>
#include <oatmi.h>
#include <ndebug.h>
#include <ondebug.h>
#include <oatmisrv_integra.h>
#include "libsrc.h"
#include <sys_unix.h>
/*---------------------------Externs------------------------------------*/
/*---------------------------Macros-------------------------------------*/
#define TPSVCINFO_CLASS     "org/endurox/TpSvcInfo"
/*---------------------------Enums--------------------------------------*/
/*---------------------------Typedefs-----------------------------------*/
/*---------------------------Globals------------------------------------*/
/*---------------------------Statics------------------------------------*/
/*---------------------------Prototypes---------------------------------*/
/**
 * Translate TPSVCINFO to java TpSvcInfo object.
 * This is complex function which also translates Client ID and 
 * 
 * @param env Java env
 * @param ctx_obj ATMI Context
 * @param is_ctxset is C context set?
 * @param svcinfo service call infos
 * @return Java object of TpSvcInfo or NULL with exception set
 */
expublic jobject ndrxj_atmi_TpSvcInfo_translate(JNIEnv *env, 
            jobject ctx_obj, int is_ctxset, TPSVCINFO *svcinfo)
{
    jobject ret = NULL;
    jclass bclz;
    jmethodID mid;
    int we_set_ctx = EXFALSE;
    
    jobject jdata;
    jobject jcltid;
    
    jstring jname;
    jstring jfname;
    
    
    /* Set context if needed */
    if (!is_ctxset)
    {
        if (NULL==ndrxj_get_ctx(env, ctx_obj, EXTRUE))
        {
            goto out;
        }
        
        we_set_ctx = EXTRUE;
    }
    
    NDRX_LOG(log_debug, "Allocating TpSvcInfo...");
    
    bclz = (*env)->FindClass(env, TPSVCINFO_CLASS);
    
    if (NULL==bclz)
    {        
        NDRX_LOG(log_error, "Failed to find class [%s]", TPSVCINFO_CLASS);
        goto out;
    }
    
    /* create buffer object... */
    mid = (*env)->GetMethodID(env, bclz, "<init>", "(Ljava/lang/String;"
            "Lorg/endurox/TypedBuffer;JIJLorg/endurox/ClientId;Ljava/lang/String;)V");
    
    if (NULL==mid)
    {
        NDRX_LOG(log_error, "Cannot get %s constructor!", TPSVCINFO_CLASS);
        goto out;
    }
    
    /* Translate ATMI buffer */
    if (NULL==(jdata=ndrxj_atmi_TypedBuffer_translate(env, 
            ctx_obj, EXTRUE, svcinfo->data, svcinfo->len,
            NULL, NULL, EXFALSE)))
    {
        NDRX_LOG(log_error, "Failed to translate ATMI buffer to Java object: %p",
                svcinfo->data);
        ret = NULL;
        goto out;
    }
    
    /* Translate ClientId */
    if (NULL==(jcltid=ndrxj_atmi_ClientId_translate(env, 
            ctx_obj, EXTRUE, &svcinfo->cltid)))
    {
        NDRX_LOG(log_error, "Failed to translate ClientId to Java object: [%s]",
                svcinfo->cltid.clientdata);
        ret = NULL;
        goto out;
    }
    
    jname = (*env)->NewStringUTF(env, svcinfo->name);
    jfname = (*env)->NewStringUTF(env, svcinfo->fname);
    
    NDRX_LOG(log_debug, "About to NewObject() of TpSvcInfo");
    
    ret = (*env)->NewObject(env, bclz, mid, jname, jdata, 
            (jlong)svcinfo->flags, (jint)svcinfo->cd, 
            (jlong)svcinfo->appkey, jcltid, jfname);
    
    if (NULL==ret)
    {
        NDRX_LOG(log_error, "Failed to create [%s] instance", TPSVCINFO_CLASS);
        goto out;
    }
    
    NDRX_LOG(log_debug, "NewObject() done of TpSvcInfo");
    
out:
    
    if (we_set_ctx)
    {
        /* return back to NULL */
        tpsetctxt(TPNULLCONTEXT, 0L);
    }

    return ret;
}

/* vim: set ts=4 sw=4 et cindent: */
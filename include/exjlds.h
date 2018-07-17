/**
 * @brief Endruo/X Java Linker, static API
 *
 * @file exjlds.h
 */ 
/*
 * -----------------------------------------------------------------------------
 * Enduro/X Middleware Platform for Distributed Transaction Processing
 * Copyright (C) 2015-2018 Mavimax, Ltd. All Rights Reserved.
 * This software is released under one of the following licenses:
 * GPL or Mavimax's license for commercial use.
 * -----------------------------------------------------------------------------
 * GPL license:
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
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

#ifndef EXJLDS_H__
#define EXJLDS_H__

#ifdef  __cplusplus
extern "C" {
#endif


/*---------------------------Includes-----------------------------------*/
#include <stdlib.h>
#include <ndrx_java_config.h>
/*---------------------------Externs------------------------------------*/
/*---------------------------Macros-------------------------------------*/
/*---------------------------Enums--------------------------------------*/
/*---------------------------Typedefs-----------------------------------*/

/**
 * Class index  
 */
struct ndrxj_class_index
{
        const char *cls;
        const char *data;
        size_t len;
};

typedef struct ndrxj_class_index ndrxj_class_index_t;

/*---------------------------Globals------------------------------------*/
/*---------------------------Statics------------------------------------*/
/*---------------------------Prototypes---------------------------------*/

extern int ndrxj_run_main(int argc, char **argv,char *main_class,
                ndrxj_class_index_t *class_index,
                int class_index_len, ndrxj_class_index_t *emb_index, int emb_index_len,
                int test_mode);

#ifdef  __cplusplus
}
#endif

#endif  /* EXJLDS_H__ */


/* vim: set ts=4 sw=4 et cindent: */

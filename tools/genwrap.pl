#!/usr/bin/perl
##
## @brief @(#) Generate wrapper code for Enduro/X C side functions
##  this parses header files, exports the wrapper header and
##  produces the dynamic loader and function lookup sources for
##  the loader library.
##  To use the script, just pass the jni generate C header file to it (names)
##
## @file genwrap.pl
##
## -----------------------------------------------------------------------------
## Enduro/X Middleware Platform for Distributed Transaction Processing
## Copyright (C) 2009-2016, ATR Baltic, Ltd. All Rights Reserved.
## Copyright (C) 2017-2018, Mavimax, Ltd. All Rights Reserved.
## This software is released under one of the following licenses:
## AGPL or Mavimax's license for commercial use.
## -----------------------------------------------------------------------------
## AGPL license:
## 
## This program is free software; you can redistribute it and/or modify it under
## the terms of the GNU Affero General Public License, version 3 as published
## by the Free Software Foundation;
##
## This program is distributed in the hope that it will be useful, but WITHOUT ANY
## WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
## PARTICULAR PURPOSE. See the GNU Affero General Public License, version 3
## for more details.
##
## You should have received a copy of the GNU Affero General Public License along 
## with this program; if not, write to the Free Software Foundation, Inc., 
## 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
##
## -----------------------------------------------------------------------------
## A commercial use license is available from Mavimax, Ltd
## contact@mavimax.com
## -----------------------------------------------------------------------------
##
use File::Basename;


@ARGS = '-' if !@ARGV;

#
# List of functions
#
@AoH = ();
  

#
# Have a list of headers which actually emitted exports
#
@M_usedhdrs = ();

#
# Read all CLI files..
#
for my $qfn (@ARGV) {
	open($fh, $qfn);

        my $firsthdr = 0;

	while (<$fh>) {
		chomp;
		my $line = $_;
		my $func = "";
		my $typ = "";
		my $sign = "";
		my $signv = "";
		my $vargs = "";
		printf( "got: [$line]\n");
		
		if ($line =~ /^JNIEXPORT/)
		{
			if (!$firsthdr)
			{
				my $hdr_nm = basename($qfn);
				push @M_usedhdrs, $hdr_nm;
				$firsthdr = 1;
			}
			($typ, $func) = $line =~ /^JNIEXPORT (.*) JNICALL (.*)/g;
			my $line2 = <$fh>;
			chomp $line2;
			
			printf("got line 2 [$line2]\n");
			($sign) = $line2 =~ /\((.*)\)/g;
			
			# Parse signature
			my @fargs = split(',', $sign);
			my $argnum = 0;
			
			foreach my $farg (@fargs) {
				print "got farg: $farg\n";
				
				if ($argnum > 0) {
					$signv = $signv.", ";
					$vargs = $vargs.", ";
				} 
				
				$vargs = $vargs."v$argnum";
				$signv = $signv."$farg v$argnum";
				$argnum++;
			}
			
			printf("func: [$func] type [$typ] sign [$sign] signv [$signv] vargs [$vargs]\n");
			
			# Fill up the array of hashes...
			push @AoH, { func => "$func", 
				type => "$typ", 
				sign => "$sign", 
				signv=> "$signv",
				vargs=> "$vargs"};
		}
	}
	close($fh);
}

#
# Now plot the loader.c
#

my $filename = '../libsrc/c/loader.c';

open(OUTC, ">$filename") or die "Could not open file '$filename' $!";


print OUTC << 'BLOCK';
/**
 * @brief AUTO GENERATED BY `genwrap.pl'! DO NOT EDIT!
 *  This is wrapper for libexjava.so so that we expose loaded symbols
 *  globally. Otherwise XA drivers from SO cannot access the enduro/x symbols.
 *
 * @file loader.c
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
#include <thlock.h>

#include <tmenv.h>
#include <libsrc.h>

#include <xa.h>
#define __USE_GNU
#include <dlfcn.h>
/* List of pointers to loaded library functions: */
BLOCK

for my $qfn (@M_usedhdrs) {
	print OUTC "#include <$qfn>\n";
}

print OUTC << 'BLOCK';
/*---------------------------Externs------------------------------------*/
/*---------------------------Macros-------------------------------------*/
/*---------------------------Enums--------------------------------------*/
/*---------------------------Typedefs-----------------------------------*/

/**
 * Symbol mapping table
 */
struct ndrxj_loader_map
{
    char    *symbol;        /**< Symbol name                                  */
    void    **funcptr;      /**< Pointer to function                          */
};
typedef struct ndrxj_loader_map ndrxj_loader_map_t;


/*---------------------------Globals------------------------------------*/
/*---------------------------Statics------------------------------------*/

/* List of pointers to loaded library functions: */
BLOCK

# Declare pionters
foreach my $row (@AoH) {
	printf OUTC "exprivate ${$row}{type} (*p_ndrxj_${$row}{func})\n";
	printf OUTC "        (${$row}{signv}) = NULL;\n";
}


# Generated mapping tabls
print OUTC << 'BLOCK';
/**
 * Function mapping table
 */
exprivate ndrxj_loader_map_t M_funcmap[] =
{  
    /* generated list of maps: */
BLOCK

foreach my $row (@AoH) {
	printf OUTC "    {\"ndrxj_${$row}{func}\", (void *)&p_ndrxj_${$row}{func}},\n";
}

# Add the loader source
print OUTC << 'BLOCK';
    {NULL, NULL}
};
    
/**
 * Set to true if library is initialize / ptrs resolved
 */
exprivate int volatile M_lib_init = EXFALSE;
exprivate void * M_handle = NULL;

/**
 * Lock for init
 */
MUTEX_LOCKDECL(M_lib_init_lock);

/*---------------------------Prototypes---------------------------------*/

/**
 * Throw exception
 * @param env java env
 * @param msg message
 */
exprivate void ndrxj_lite_exception(JNIEnv *env, char *msg)
{
    (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/Exception"), msg);
}

/**
 * Perform library init with mutex lock to avoid twice init - protect from
 * threaded access
 * @param[in] env Java env
 * @return EXSUCCEED/EXFAIL
 */
exprivate int ndrxj_lib_init(JNIEnv *env)
{
    int ret = EXSUCCEED;
    char buf[512];
#if EX_OS_DARWIN   
    char *lib = "libexjava.dylib";
#else
    char *lib = "libexjava.so";
#endif
    int i;
    
    MUTEX_LOCK_V(M_lib_init_lock);
    
    if (!M_lib_init)
    {
        /* check the symbol, if have one, then load directly
         * if does not have symbol, then load the library
         * Symbol mapping shall be done in separate function.
         */
        
        if (NULL==dlsym( RTLD_DEFAULT, "ndrxj_Java_org_endurox_AtmiCtx_tpnewctxt" ))
        {
            /* load shared library */
            M_handle = dlopen (lib, RTLD_NOW | RTLD_GLOBAL);
            
            if (!M_handle)
            {
                snprintf(buf, sizeof(buf), "Failed to load %s: %s", lib, dlerror());
                ndrxj_lite_exception(env, buf);
                EXFAIL_OUT(ret);
            }
        }
        
        /* load mappings, all maps shall succeed, else we give error */
        for (i=0; NULL!=M_funcmap[i].symbol; i++)
        {
            (*M_funcmap[i].funcptr) = dlsym(M_handle, M_funcmap[i].symbol);
            
            if (NULL==(*M_funcmap[i].funcptr))
            {
                snprintf(buf, sizeof(buf), "Failed to resolve `%s' function", 
                        M_funcmap[i].symbol);
                ndrxj_lite_exception(env, buf);
                EXFAIL_OUT(ret);
            }
        }
    }
    
out:
    MUTEX_UNLOCK_V(M_lib_init_lock);
    
    return ret;
}

BLOCK

# 
# now generate functions..., non void and void two kinds
#

foreach my $row (@AoH) {

	if ( ${$row}{type} eq "void") {
		print OUTC << "BLOCK";
/**
 * Auto generated
 */
expublic ${$row}{type} JNICALL ${$row}{func} (${$row}{signv})
{
    if (!M_lib_init)
    {
        if (EXSUCCEED!=ndrxj_lib_init(v0))
        {
            return;
        }
    }
    
    p_ndrxj_${$row}{func}(${$row}{vargs});
}

BLOCK
	}
	else
	{
		# This is non void func
		print OUTC << "BLOCK";
/**
 * Auto generated
 */
expublic ${$row}{type} JNICALL ${$row}{func} (${$row}{signv})
{
    if (!M_lib_init)
    {
        if (EXSUCCEED!=ndrxj_lib_init(v0))
        {
            return (${$row}{type})0;
        }
    }
    
    return p_ndrxj_${$row}{func}(${$row}{vargs});
}

BLOCK
	}
}

#
# Close C
#

# Generated mapping tabls
print OUTC << 'BLOCK';
#undef __USE_GNU
/* vim: set ts=4 sw=4 et smartindent: */
BLOCK

close (OUTC);


#
# Now generate header file for real glue code
#

$filename='../include/exjglue.h';

open(OUTH, ">$filename") or die "Could not open file '$filename' $!";


print OUTH << 'BLOCK';
/**
 * @brief AUTO GENERATED BY `genwrap.pl'. DO NOT EDIT! This is header file
 *  is used by manually written source code for Java bindings.
 *
 * @file exjglue.h
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
 
#ifndef NDRXJ_EXGLUE_H__
#define NDRXJ_EXGLUE_H__

#ifdef  __cplusplus
extern "C" {
#endif

/*---------------------------Includes-----------------------------------*/
#include <jni.h>
#include <ndrx_java_config.h>
/*---------------------------Externs------------------------------------*/
/*---------------------------Macros-------------------------------------*/
/*---------------------------Enums--------------------------------------*/
/*---------------------------Typedefs-----------------------------------*/
/*---------------------------Globals------------------------------------*/
/*---------------------------Statics------------------------------------*/
/*---------------------------Prototypes---------------------------------*/

BLOCK


# Declare pionters
foreach my $row (@AoH) {
	printf OUTH "extern NDRX_JAVA_API ${$row}{type} JNICALL ndrxj_${$row}{func}\n";
	printf OUTH "        (${$row}{sign});\n";
}


print OUTH << 'BLOCK';

#endif  /* NDRXJ_EXGLUE_H__ */

/* vim: set ts=4 sw=4 et smartindent: */
BLOCK

close(OUTH);

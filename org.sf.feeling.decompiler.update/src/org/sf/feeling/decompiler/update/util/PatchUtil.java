/*******************************************************************************
 * Copyright (c) 2017 Chen Chao(cnfree2000@hotmail.com).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Chen Chao  - initial API and implementation
 *******************************************************************************/

package org.sf.feeling.decompiler.update.util;

import java.io.File;
import java.io.FileFilter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.Version;
import org.sf.feeling.decompiler.JavaDecompilerPlugin;
import org.sf.feeling.decompiler.update.DecompilerUpdatePlugin;
import org.sf.feeling.decompiler.util.FileUtil;
import org.sf.feeling.decompiler.util.Logger;
import org.sf.feeling.decompiler.util.ReflectionUtils;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

public class PatchUtil
{

	private static final String DECOMPILER_FRAGMENT_ID = "org.sf.feeling.decompiler.fragment";
	public static final String DEFAULT_PATCH_PLUGIN_ID = "org.sf.feeling.decompiler.patch"; //$NON-NLS-1$

	public static File getLatestPatch( File patchFolder, final String patchId )
	{
		if ( patchFolder != null )
		{
			File[] children = patchFolder.listFiles( new FileFilter( ) {

				public boolean accept( File file )
				{
					if ( file.isDirectory( ) )
						return false;
					if ( getPatchFileVersion( file, patchId ) != null )
						return true;
					return false;
				}
			} );

			if ( children != null && children.length > 0 )
			{
				if ( children.length > 1 )
				{
					Arrays.sort( children, new Comparator<File>( ) {

						public int compare( File o1, File o2 )
						{
							Version v1 = getPatchFileVersion( o1, patchId );
							Version v2 = getPatchFileVersion( o2, patchId );
							return v2.compareTo( v1 );
						}
					} );
				}
				return children[0];
			}
		}
		return null;
	}

	public static File getLatestFragment( File patchFolder, final String patchId )
	{
		if ( patchFolder != null )
		{
			File[] children = patchFolder.listFiles( new FileFilter( ) {

				public boolean accept( File file )
				{
					if ( file.isDirectory( ) )
						return false;
					if ( getFileVersion( file, patchId ) != null )
						return true;
					return false;
				}
			} );

			if ( children != null && children.length > 0 )
			{
				if ( children.length > 1 )
				{
					Arrays.sort( children, new Comparator<File>( ) {

						public int compare( File o1, File o2 )
						{
							Version v1 = getFileVersion( o1, patchId );
							Version v2 = getFileVersion( o2, patchId );
							return v2.compareTo( v1 );
						}
					} );
				}
				return children[0];
			}
		}
		return null;
	}

	public static Version getLocalPatchVersion( File patchFolder, final String patchId )
	{
		if ( patchFolder != null )
		{
			File[] children = patchFolder.listFiles( new FileFilter( ) {

				public boolean accept( File file )
				{
					if ( file.isDirectory( ) )
						return false;
					if ( getPatchFileVersion( file, patchId ) != null )
						return true;
					return false;
				}
			} );

			if ( children != null && children.length > 0 )
			{
				if ( children.length > 1 )
				{
					Arrays.sort( children, new Comparator<File>( ) {

						public int compare( File o1, File o2 )
						{
							Version v1 = getPatchFileVersion( o1, patchId );
							Version v2 = getPatchFileVersion( o2, patchId );
							return v2.compareTo( v1 );
						}
					} );
				}
				return getPatchFileVersion( children[0], patchId );
			}
		}
		return null;
	}

	private static Version getPatchFileVersion( File file, String patchId )
	{
		if ( patchId == null )
		{
			patchId = DEFAULT_PATCH_PLUGIN_ID;
		}

		Bundle bundle = Platform.getBundle( patchId );
		if ( bundle != null )
		{
			return bundle.getVersion( );
		}

		return Version.parseVersion( file.getName( )
				.toLowerCase( )
				.replace( patchId + "_", "" ) //$NON-NLS-1$ //$NON-NLS-2$
				.replace( ".jar", "" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static Version getFileVersion( File file, String patchId )
	{
		return Version.parseVersion( file.getName( )
				.toLowerCase( )
				.replace( patchId + "_", "" ) //$NON-NLS-1$ //$NON-NLS-2$
				.replace( ".jar", "" ) ); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static boolean installPatch( File file )
	{
		BundleContext context = FrameworkUtil.getBundle( JavaDecompilerPlugin.class ).getBundleContext( );
		if ( DecompilerUpdatePlugin.getDefault( ).getPatchFile( ) != null )
		{
			if ( DecompilerUpdatePlugin.getDefault( ).getPatchFile( ).equals( file ) )
				return true;
			try
			{
				Bundle bundle = (Bundle) ReflectionUtils.invokeMethod( context, "getBundle", new Class[]{ //$NON-NLS-1$
						String.class
				}, new Object[]{
						DecompilerUpdatePlugin.getDefault( ).getPatchFile( ).toURI( ).toString( )
				} );

				if ( bundle == null )
				{
					return false;
				}
				bundle.uninstall( );
			}
			catch ( BundleException e )
			{
				Logger.debug( e );
			}
		}
		try
		{
			Bundle bundle = context.installBundle( file.toURI( ).toString( ) );
			if ( bundle != null )
			{
				bundle.start( );
				DecompilerUpdatePlugin.getDefault( ).setPatchFile( file );
				return true;
			}
		}
		catch ( BundleException e )
		{
			Logger.debug( e );
		}
		return false;
	}

	public static String[] loadPatchIds( )
	{
		String patchIds = JavaDecompilerPlugin.getDefault( ).getPreferenceStore( ).getString( "patchIds" ); //$NON-NLS-1$
		if ( patchIds != null )
		{
			return patchIds.split( "," ); //$NON-NLS-1$
		}
		return new String[0];
	}

	public static void savePatchIds( List<String> patchIds )
	{
		if ( patchIds != null && patchIds.size( ) > 0 )
		{
			JavaDecompilerPlugin.getDefault( ).getPreferenceStore( ).setValue( "patchIds", //$NON-NLS-1$
					Arrays.toString( patchIds.toArray( new String[0] ) )
							.replace( "[", "" ) //$NON-NLS-1$ //$NON-NLS-2$
							.replace( "]", "" ) //$NON-NLS-1$ //$NON-NLS-2$
							.trim( ) );
		}
	}

	public static boolean checkPatch( String patchId, String version, String downloadUrl )
	{
		try
		{
			IPath path = JavaDecompilerPlugin.getDefault( ).getStateLocation( );
			IPath patchDir = path.append( "patch" ); //$NON-NLS-1$
			File patchFolder = patchDir.toFile( );
			if ( !patchFolder.exists( ) )
			{
				patchFolder.mkdirs( );
				return downloadFile( patchFolder, patchId, version, downloadUrl );
			}
			else
			{
				Version localVerion = PatchUtil.getLocalPatchVersion( patchFolder, patchId );
				Version remoteVersion = Version.parseVersion( version );
				if ( remoteVersion != null )
				{
					if ( localVerion == null || remoteVersion.compareTo( localVerion ) > 0 )
					{
						return downloadFile( patchFolder, patchId, version, downloadUrl );
					}
					else
						return true;
				}
				else
					return true;
			}
		}
		catch ( Throwable e )
		{
			return false;
		}
	}

	private static boolean downloadFile( File patchFolder, String patchId, String version, String downloadUrl )
	{
		File downloadFile = new File( patchFolder, patchId + "_" + version + ".jar" );//$NON-NLS-1$ //$NON-NLS-2$
		try
		{
			URL location = new URL( downloadUrl ); // $NON-NLS-1$
			HttpURLConnection con = (HttpURLConnection) location.openConnection( );
			con.setRequestMethod( "GET" ); //$NON-NLS-1$
			con.setRequestProperty( "User-Agent", "Mozilla/5.0" ); // $NON-NLS-1$ //$NON-NLS-1$//$NON-NLS-2$

			int responseCode = con.getResponseCode( );
			if ( responseCode == HttpURLConnection.HTTP_OK )
			{
				if ( patchId == null )
				{
					patchId = PatchUtil.DEFAULT_PATCH_PLUGIN_ID;
				}
				FileUtil.writeToBinarayFile( downloadFile, con.getInputStream( ), true );
				con.disconnect( );
				return true;
			}
			else
			{
				con.disconnect( );
				return false;
			}
		}
		catch ( Exception e )
		{
			if ( downloadFile.exists( ) )
			{
				downloadFile.delete( );
			}
			return false;
		}
	}

	public static boolean loadPatch( )
	{
		final boolean[] result = new boolean[]{
				true
		};

		Display.getDefault( ).syncExec( new Runnable( ) {

			public void run( )
			{
				IPath path = JavaDecompilerPlugin.getDefault( ).getStateLocation( );
				IPath patchDir = path.append( "patch" ); //$NON-NLS-1$
				File patchFolder = patchDir.toFile( );
				if ( !patchFolder.exists( ) )
					patchFolder.mkdirs( );
				String[] patchIds = PatchUtil.loadPatchIds( );
				if ( patchIds != null )
				{
					for ( int i = 0; i < patchIds.length; i++ )
					{
						String patchId = patchIds[i].trim( );
						File patchFile = PatchUtil.getLatestPatch( patchFolder, patchId );
						if ( patchFile != null && patchFile.exists( ) && patchFile.isFile( ) )
						{
							if ( !PatchUtil.installPatch( patchFile ) )
								result[0] = false;
						}
					}
				}
			}
		} );

		return result[0];
	}

	public static boolean loadFragment( )
	{
		final boolean[] result = new boolean[]{
				true
		};

		Display.getDefault( ).syncExec( new Runnable( ) {

			public void run( )
			{
				try
				{
					String eclipseHome = System.getProperty( "eclipse.home.location" );
					if ( eclipseHome == null )
						return;
					File eclipseDir = new File( new URL( eclipseHome ).toURI( ) );
					File dropinDir = new File( eclipseDir, "dropins" );
					if ( dropinDir.exists( ) )
					{
						IPath path = JavaDecompilerPlugin.getDefault( ).getStateLocation( );
						IPath fragmentDir = path.append( "fragment" ); //$NON-NLS-1$
						File fragmentFolder = fragmentDir.toFile( );
						File fragmentFile = getLatestFragment( fragmentFolder, DECOMPILER_FRAGMENT_ID );
						if ( fragmentFile != null && fragmentFile.exists( ) && fragmentFile.isFile( ) )
						{
							File pluginsDir = new File( dropinDir, "eclipse/plugins" );
							if ( !pluginsDir.exists( ) )
							{
								pluginsDir.mkdirs( );
							}
							File file = getLatestFragment( pluginsDir, DECOMPILER_FRAGMENT_ID );
							if ( file != null && file.exists( ) )
							{
								Version newVersion = getFileVersion( fragmentFile, DECOMPILER_FRAGMENT_ID );
								Version oldVersion = getFileVersion( file, DECOMPILER_FRAGMENT_ID );
								if ( oldVersion == null )
								{
									FileUtil.copyFile( fragmentFile.getAbsolutePath( ),
											new File( pluginsDir, fragmentFile.getName( ) ).getAbsolutePath( ) );
								}
								else if ( newVersion.compareTo( oldVersion ) > 0 )
								{
									if ( FileUtil.copyFile( fragmentFile.getAbsolutePath( ),
											new File( pluginsDir, fragmentFile.getName( ) ).getAbsolutePath( ) ) )
									{
										fragmentFile.delete( );
									} ;
									file.deleteOnExit( );
								}
							}
							else
							{
								FileUtil.copyFile( fragmentFile.getAbsolutePath( ),
										new File( pluginsDir, fragmentFile.getName( ) ).getAbsolutePath( ) );
								fragmentFile.delete( );
							}
						}
					}
				}
				catch ( Exception e )
				{
					Logger.debug( e );
				}
			}
		} );

		return result[0];
	}

	public static boolean handlePatchJson( JsonValue patchValue, List<String> patchIds )
	{
		if ( patchValue instanceof JsonArray )
		{
			JsonArray patchs = (JsonArray) patchValue;
			boolean result = true;
			for ( int i = 0; i < patchs.size( ); i++ )
			{
				if ( !handlePatchJson( patchs.get( i ), patchIds ) )
				{
					result = false;
				}
			}
			return result;
		}
		else if ( patchValue instanceof JsonObject )
		{
			JsonObject patch = (JsonObject) patchValue;
			String version = patch.getString( "version", null ); //$NON-NLS-1$
			String url = patch.getString( "url", null ); //$NON-NLS-1$
			String id = patch.getString( "id", null ); //$NON-NLS-1$
			if ( version != null && url != null )
			{
				patchIds.add( id );
				return PatchUtil.checkPatch( id, version, url );
			}
		}
		return false;
	}

	public static boolean handleFragmentJson( JsonValue fragmentValue )
	{
		if ( fragmentValue instanceof JsonObject )
		{
			JsonObject patch = (JsonObject) fragmentValue;
			String version = patch.getString( "version", null ); //$NON-NLS-1$
			String url = patch.getString( "url", null ); //$NON-NLS-1$
			String id = patch.getString( "id", null ); //$NON-NLS-1$
			if ( version != null && url != null )
			{
				return PatchUtil.checkFragment( id, version, url );
			}
		}
		return false;
	}

	private static boolean checkFragment( String fragmentId, String version, String downloadUrl )
	{
		try
		{
			IPath path = JavaDecompilerPlugin.getDefault( ).getStateLocation( );
			IPath fragmentDir = path.append( "fragment" ); //$NON-NLS-1$
			File fragmentFolder = fragmentDir.toFile( );
			if ( !fragmentFolder.exists( ) )
			{
				fragmentFolder.mkdirs( );
				return downloadFile( fragmentFolder, fragmentId, version, downloadUrl );
			}
			else
			{
				Version localVerion = getFragmentVersion( );
				Version remoteVersion = Version.parseVersion( version );
				if ( remoteVersion != null )
				{
					if ( localVerion == null || remoteVersion.compareTo( localVerion ) > 0 )
					{
						return downloadFile( fragmentFolder, fragmentId, version, downloadUrl );
					}
					else
						return true;
				}
				else
					return true;
			}
		}
		catch ( Throwable e )
		{
			return false;
		}
	}

	public static Version getFragmentVersion( )
	{
		Bundle bundle = Platform.getBundle( DECOMPILER_FRAGMENT_ID );
		if ( bundle != null )
		{
			return bundle.getVersion( );
		}
		return null;
	}

	public static String getFragment( )
	{
		Version version = getFragmentVersion( );
		return version == null ? null : version.toString( );
	}

}

package com.google.code.orion_viewer;

/*Orion Viewer is a pdf viewer for Nook Classic based on mupdf

Copyright (C) 2011  Michael Bogdanov

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Debug;
import android.util.Log;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * User: mike
 * Date: 19.10.11
 * Time: 9:52
 */
public class RenderThread extends Thread {

    private LayoutStrategy layout;

    private LinkedList<CacheInfo> cachedBitmaps = new LinkedList<CacheInfo>();

    private OrionView view;

    private LayoutPosition currentPosition;

    private LayoutPosition lastEvent;

    private DocumentWrapper doc;

    private int CACHE_SIZE = 4;

    private int FUTURE_COUNT = 1;

    private Canvas cacheCanvas = new Canvas();

    private Bitmap.Config bitmapConfig = Bitmap.Config.ARGB_8888;

    private boolean clearCache;

    private int rotationShift;

    private boolean stopped;

    private boolean paused;

    public RenderThread(OrionView view, LayoutStrategy layout, DocumentWrapper doc) {
        this.view = view;
        this.layout = layout;
        this.doc = doc;
        rotationShift = 80;
    }


    public void invalidateCache() {
        synchronized (this) {
            //if(clearCache) {
              //  clearCache = false;
                for (Iterator<CacheInfo> iterator = cachedBitmaps.iterator(); iterator.hasNext(); ) {
                    CacheInfo next = iterator.next();
                    next.bitmap.recycle();
                    next.bitmap = null;
                }

                Log.d(Common.LOGTAG, "Clean cache");
                Log.d(Common.LOGTAG, "Allocated heap size " + (Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()));
                Log.d(Common.LOGTAG, "Total free memory " + Runtime.getRuntime().freeMemory());
                cachedBitmaps.clear();
                Log.d(Common.LOGTAG, "Cache is cleared!");
            //}


            currentPosition = null;
            //clearCache = true;
        }
    }


    public void cleanCache() {
        synchronized (this) {
            //if(clearCache) {
              //  clearCache = false;
                for (Iterator<CacheInfo> iterator = cachedBitmaps.iterator(); iterator.hasNext(); ) {
                    CacheInfo next = iterator.next();
                    next.bitmap.recycle();
                    next.bitmap = null;
                }

                Log.d(Common.LOGTAG, "Clean cache");
                Log.d(Common.LOGTAG, "Allocated heap size " + (Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()));
                Log.d(Common.LOGTAG, "Total free memory " + Runtime.getRuntime().freeMemory());
                cachedBitmaps.clear();
                Log.d(Common.LOGTAG, "Cache is cleared!");
            //}


            currentPosition = null;
            //clearCache = true;
        }
    }

    public void stopRenderer() {
        synchronized (this) {
            stopped = true;
            cleanCache();
            notify();
        }
    }

    public void onPause() {
        synchronized (this) {
            paused = true;
        }
    }

    public void onResume() {
        synchronized (this) {
            paused = false;
            notify();
        }
    }

    public void run() {
        int futureIndex = 0;
        LayoutPosition curPos = null;

        while (!stopped) {

            Log.d(Common.LOGTAG, "Allocated heap size1 " + (Debug.getNativeHeapAllocatedSize() - Debug.getNativeHeapFreeSize()));
            Log.d(Common.LOGTAG, "Total appliication memory1 " + Runtime.getRuntime().totalMemory());
            Log.d(Common.LOGTAG, "Total free memory1 " + Runtime.getRuntime().freeMemory());

            int rotation = 0;
            synchronized (this) {
                if (paused) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    continue;
                }

                if (lastEvent != null) {
                    currentPosition = lastEvent;
                    lastEvent = null;
                    futureIndex = 0;
                    curPos = currentPosition;
                }

                //keep it here
                rotation = layout.getRotation();

                if (currentPosition == null || futureIndex > FUTURE_COUNT) {
                    try {
                        Log.d(Common.LOGTAG, "WAITING...");
                        wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.d(Common.LOGTAG, "AWAKENING!!!");
                    continue;
                } else {
                    //will cache next page
                    Log.d(Common.LOGTAG, "Future index is " + futureIndex);
                    if (futureIndex != 0) {
                        curPos = curPos.clone();
                        layout.nextPage(curPos);
                    }
                }
            }

            if (curPos != null) {
                CacheInfo resultEntry = null;
                //try to find result in cache
                for (Iterator<CacheInfo> iterator = cachedBitmaps.iterator(); iterator.hasNext(); ) {
                    CacheInfo cacheInfo =  iterator.next();
                    if (cacheInfo.info.equals(curPos)) {
                        resultEntry = cacheInfo;
                        //relocate info to end of cache
                        iterator.remove();
                        cachedBitmaps.add(cacheInfo);
                        break;
                    }
                }


                if (resultEntry == null) {
                    //render page
                    int width = curPos.pieceWidth;
                    int height = curPos.pieceHeight;

                    Bitmap bitmap = null;
                    if (cachedBitmaps.size() >= CACHE_SIZE) {
                        CacheInfo info = cachedBitmaps.removeFirst();
                        if (width == info.bitmap.getWidth() && height == info.bitmap.getHeight() || rotation != 0 && width == info.bitmap.getHeight() && height == info.bitmap.getWidth()) {
                            bitmap = info.bitmap;
                        } else {
                            info.bitmap.recycle(); //todo recycle from ui
                            info.bitmap = null;
                        }
                    }
                    if (bitmap == null) {
//                        try {
                            Log.d(Common.LOGTAG, "CREATING BITMAP!!!");
                            bitmap = Bitmap.createBitmap(SimpleLayoutStrategy.WIDTH, SimpleLayoutStrategy.HEIGHT, bitmapConfig);
//                        } catch (OutOfMemoryError e) {
//                            System.gc();
//                        }
                    }

                    cacheCanvas.setMatrix(null);
                    if (rotation != 0) {
                        cacheCanvas.rotate(-rotation * 90, (view.getHeight()) / 2, view.getWidth() / 2);
                        cacheCanvas.translate(-rotation * rotationShift, -rotation * rotationShift);
                    }

                    int [] data = doc.renderPage(curPos.pageNumber, curPos.docZoom, width, height, curPos.offsetX, curPos.offsetY, curPos.offsetX + width, curPos.offsetY + height);                    cacheCanvas.setBitmap(bitmap);

                    cacheCanvas.drawBitmap(data, 0, width, 0, 0, width, height, true, null);

                    resultEntry = new CacheInfo(curPos, bitmap);

                    cachedBitmaps.add(resultEntry);
                }

                if (futureIndex == 0) {
                    //redraw view
                    //TODO send event from ui thread
                    view.setData(resultEntry.bitmap);
                    view.postInvalidate();
                }

                futureIndex++;
            }
        }
    }

    public void render(LayoutPosition lastInfo) {
        lastInfo = lastInfo.clone();
        synchronized (this) {
            lastEvent = lastInfo;
            notify();
        }
    }

    static class CacheInfo {

        public CacheInfo(LayoutPosition info, Bitmap bitmap) {
            this.info  = info;
            this.bitmap = bitmap;
        }

        private LayoutPosition info;
        private Bitmap bitmap;

        private boolean valid;

        public LayoutPosition getInfo() {
            return info;
        }

        public Bitmap getBitmap() {
            return bitmap;
        }

    }
}

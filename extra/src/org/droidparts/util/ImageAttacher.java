/**
 * Copyright 2011 Alex Yanchenko
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package org.droidparts.util;

import static org.droidparts.util.IOUtils.silentlyClose;

import java.io.BufferedInputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.droidparts.net.http.HTTPException;
import org.droidparts.net.http.RESTClient;

import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public final class ImageAttacher {

	private final ExecutorService exec;
	private final RESTClient client;
	private final FileCacher fileCacher;

	public ImageAttacher(FileCacher fileCacher) {
		exec = Executors.newSingleThreadExecutor();
		client = new RESTClient(null);
		this.fileCacher = fileCacher;
	}

	public void setImage(final String fileUrl, final View view) {
		exec.execute(new Runnable() {

			@Override
			public void run() {
				final Drawable image = get(fileUrl);
				if (image != null) {
					view.post(new AttachRunnable(view, image));
				}
			}
		});
	}

	private Drawable get(String fileUrl) {

		BitmapDrawable image = null;
		if (fileCacher != null) {
			image = fileCacher.readFromCache(fileUrl);
		}

		if (image == null) {
			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(client.getInputStream(fileUrl).v);
				image = new BitmapDrawable(bis);
			} catch (HTTPException e) {
				L.e(e);
			} finally {
				silentlyClose(bis);
			}

			if (fileCacher != null && image != null) {
				fileCacher.saveToCache(fileUrl, image);
			}
		}
		return image;
	}

	private static class AttachRunnable implements Runnable {

		private final View view;
		private final Drawable drawable;

		public AttachRunnable(View view, Drawable drawable) {
			this.view = view;
			this.drawable = drawable;
		}

		@Override
		public void run() {
			if (view instanceof ImageView) {
				ImageView imageView = (ImageView) view;
				imageView.setImageDrawable(drawable);
			} else if (view instanceof TextView) {
				TextView textView = (TextView) view;
				textView.setCompoundDrawablesWithIntrinsicBounds(null,
						drawable, null, null);
			} else {
				L.e("Unsupported type: " + view.getClass());
			}
		}

	}

}
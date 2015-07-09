/*
 * Copyright (c) 2015, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk.module.messagecenter.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.os.AsyncTask;
import android.widget.EditText;

import com.apptentive.android.sdk.Log;
import com.apptentive.android.sdk.R;
import com.apptentive.android.sdk.model.*;
import com.apptentive.android.sdk.module.messagecenter.MessageManager;
import com.apptentive.android.sdk.module.messagecenter.model.ApptentiveMessage;
import com.apptentive.android.sdk.module.messagecenter.model.IncomingTextMessage;
import com.apptentive.android.sdk.module.messagecenter.model.MessageCenterComposingItem;
import com.apptentive.android.sdk.module.messagecenter.model.MessageCenterGreeting;
import com.apptentive.android.sdk.module.messagecenter.model.MessageCenterListItem;
import com.apptentive.android.sdk.module.messagecenter.model.MessageCenterStatus;
import com.apptentive.android.sdk.module.messagecenter.model.OutgoingFileMessage;
import com.apptentive.android.sdk.module.messagecenter.model.OutgoingTextMessage;
import com.apptentive.android.sdk.module.messagecenter.view.holder.GreetingHolder;
import com.apptentive.android.sdk.module.messagecenter.view.holder.HolderFactory;
import com.apptentive.android.sdk.module.messagecenter.view.holder.IncomingTextMessageHolder;
import com.apptentive.android.sdk.module.messagecenter.view.holder.MessageCenterListItemHolder;
import com.apptentive.android.sdk.module.messagecenter.view.holder.OutgoingFileMessageHolder;
import com.apptentive.android.sdk.module.messagecenter.view.holder.OutgoingTextMessageHolder;
import com.apptentive.android.sdk.module.messagecenter.view.holder.StatusHolder;
import com.apptentive.android.sdk.module.metric.MetricModule;
import com.apptentive.android.sdk.util.ImageUtil;
import com.apptentive.android.sdk.util.Util;

import java.io.FileInputStream;

import java.lang.ref.WeakReference;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Sky Kelsey
 */
public class MessageAdapter<T extends MessageCenterListItem> extends ArrayAdapter<T> {

	private static final int
			TYPE_TEXT_INCOMING = 0,
			TYPE_TEXT_OUTGOING = 1,
			TYPE_FILE_OUTGOING = 2,
			TYPE_GREETING = 3,
			TYPE_STATUS = 4,
			TYPE_AUTO = 5,
	    TYPE_Composing = 6;

	private static final int INVALID_POSITION = -1;

	private final static float MAX_IMAGE_SCREEN_PROPORTION_X = 0.5f;
	private final static float MAX_IMAGE_SCREEN_PROPORTION_Y = 0.6f;

	// Some absolute size limits to keep bitmap sizes down.
	private final static int MAX_IMAGE_DISPLAY_WIDTH = 800;
	private final static int MAX_IMAGE_DISPLAY_HEIGHT = 800;

	private boolean isInPauseState = false;
	private Context context;
	private int composingViewIndex = INVALID_POSITION;
	private MessageCenterComposingView composingView;
	private EditText et;

	// maps to prevent redundant asynctasks
	private ArrayList<Integer> positionsWithPendingUpdateTask = new ArrayList<Integer>();
	private ArrayList<Integer> positionsWithPendingImageTask = new ArrayList<Integer>();



	public MessageAdapter(Context context, List<MessageCenterListItem> items) {
		super(context, 0, (List<T>) items);
		this.context = context;
	}

	@Override
	public int getItemViewType(int position) {
		MessageCenterListItem listItem = getItem(position);
		if (listItem instanceof ApptentiveMessage) {
			ApptentiveMessage apptentiveMessage = (ApptentiveMessage) listItem;
			if (apptentiveMessage.getBaseType() == Payload.BaseType.message) {
				switch (apptentiveMessage.getType()) {
					case TextMessage:
						if (apptentiveMessage instanceof IncomingTextMessage) {
							return TYPE_TEXT_INCOMING;
						} else if (apptentiveMessage instanceof OutgoingTextMessage) {
							return TYPE_TEXT_OUTGOING;
						}
						break;
					case FileMessage:
						if (apptentiveMessage instanceof OutgoingFileMessage) {
							return TYPE_FILE_OUTGOING;
						}
						break;
					case AutomatedMessage:
						return TYPE_AUTO;
					default:
						break;
				}
			}
		} else if (listItem instanceof MessageCenterGreeting) {
			return TYPE_GREETING;
		} else if (listItem instanceof MessageCenterStatus) {
			return TYPE_STATUS;
		} else if (listItem instanceof MessageCenterComposingItem) {
			return TYPE_Composing;
		}
		return IGNORE_ITEM_VIEW_TYPE;
	}

	@Override
	public int getViewTypeCount() {
		return 7;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		MessageCenterListItem listItem = getItem(position);

		int type = getItemViewType(position);
		MessageCenterListItemHolder holder = null;
		boolean bLoadAvatar = false;
		if (null == convertView) {
			// TODO: Do we need this switch anymore?
			switch (type) {
				case TYPE_TEXT_INCOMING:
					convertView = new IncomingTextMessageView(parent.getContext(), (IncomingTextMessage) listItem);
					bLoadAvatar = true;
					break;
				case TYPE_TEXT_OUTGOING:
					convertView = new OutgoingTextMessageView(parent.getContext(), (OutgoingTextMessage) listItem);
					break;
/*
				case TYPE_FILE_INCOMING:
					convertView = new FileMessageView(parent.getContext(), (OutgoingFileMessage) listItem);
					break;
*/
				case TYPE_FILE_OUTGOING:
					convertView = new FileMessageView(parent.getContext(), (OutgoingFileMessage) listItem);
					break;
				case TYPE_GREETING:
					convertView = new MessageCenterGreetingView(parent.getContext(), (MessageCenterGreeting) listItem);
					break;
				case TYPE_STATUS:
					convertView = new MessageCenterStatusView(parent.getContext(), (MessageCenterStatus) listItem);
					break;
				case TYPE_Composing: {
					if (composingView == null) {
						composingView = new MessageCenterComposingView(context, position);
					}

					et = composingView.getEditText();
					et.setOnTouchListener(new View.OnTouchListener() {
						@Override
						public boolean onTouch(View v, MotionEvent event) {
							if (event.getAction() == MotionEvent.ACTION_UP) {
								composingViewIndex = position;
							}
							return false;
						}
					});
					focusOnEditText();
					convertView = composingView;
					break;
				}
				default:
					Log.i("Unrecognized type: %d", type);
					break;
			}
			if (convertView != null) {
				holder = HolderFactory.createHolder((MessageCenterListItemView) convertView);
				convertView.setTag(holder);
			}
		} else {
			holder = (MessageCenterListItemHolder) convertView.getTag();
		}

		if (holder != null) {
			switch (type) {
				case TYPE_TEXT_INCOMING: {
					if (bLoadAvatar) {
						ImageUtil.startDownloadAvatarTask(((IncomingTextMessageHolder) holder).avatar,
								((IncomingTextMessage) listItem).getSenderProfilePhoto());
					}
					final IncomingTextMessage textMessage = (IncomingTextMessage) listItem;
					String timestamp = createTimestamp(((IncomingTextMessage) listItem).getCreatedAt());
					((IncomingTextMessageHolder) holder).updateMessage(timestamp, textMessage.getBody());
					if (!textMessage.isRead() && !positionsWithPendingUpdateTask.contains(position)) {
						positionsWithPendingUpdateTask.add(position);
						startUpdateUnreadMessageTask(textMessage, position);
					}
					break;
				}
				case TYPE_TEXT_OUTGOING: {
					OutgoingTextMessage textMessage = (OutgoingTextMessage) listItem;
					String timestamp = createTimestamp(((OutgoingTextMessage) listItem).getCreatedAt());
					((OutgoingTextMessageHolder) holder).updateMessage(timestamp, textMessage.getCreatedAt() == null && !isInPauseState, textMessage.getBody());
					break;
				}
				case TYPE_FILE_OUTGOING: {
					OutgoingFileMessage fileMessage = (OutgoingFileMessage) listItem;
					if (position != holder.position && !positionsWithPendingImageTask.contains(position)) {
						positionsWithPendingImageTask.add(position);
						startLoadAttachedImageTask((OutgoingFileMessage) listItem, position, (OutgoingFileMessageHolder) holder);
					}
					String timestamp = createTimestamp(((OutgoingFileMessage) listItem).getCreatedAt());
					((OutgoingFileMessageHolder) holder).updateMessage(timestamp, fileMessage.getCreatedAt() == null);
					break;
				}
				case TYPE_GREETING:
					MessageCenterGreeting greeting = (MessageCenterGreeting) listItem;
					((GreetingHolder) holder).updateMessage(greeting.getTitle(), greeting.getBody());
					break;
				case TYPE_STATUS:
					MessageCenterStatus status = (MessageCenterStatus) listItem;
					((StatusHolder) holder).updateMessage(status.getTitle(), status.getBody());
					break;
				case TYPE_Composing:
					break;
				default:
					return null;
			}
			holder.position = position;
		}
		if (et!= null) {
			//et.clearFocus();
			if (composingViewIndex != INVALID_POSITION && composingViewIndex == position) {
				et.requestFocus();
			}
		}
		return convertView;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		return false;
	}

	public EditText getEditTextInComposing() {
		if (composingView != null) {
			return composingView.getEditText();
		}
		return null;
	}

	public void focusOnEditText() {
		EditText et = composingView.getEditText();
		et.requestFocus();
	}

	public void clearComposing() {
		composingView = null;
	}

	public void setPaused(boolean bPause) {
		isInPauseState = bPause;
	}

	protected String createTimestamp(Double seconds) {
		if (seconds != null) {
			Date date = new Date(Math.round(seconds * 1000));
			DateFormat mediumDateShortTimeFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
			return mediumDateShortTimeFormat.format(date);
		}

		int resId = isInPauseState ? R.string.apptentive_paused : R.string.apptentive_sending;
		return context.getResources().getString(resId);
	}


	private Point getBitmapDimensions(StoredFile storedFile) {
		Point ret = null;
		FileInputStream fis = null;
		try {
			fis = context.openFileInput(storedFile.getLocalFilePath());

			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(fis, null, options);

			Point point = Util.getScreenSize(context);
			int maxImageWidth = (int) (MAX_IMAGE_SCREEN_PROPORTION_X * point.x);
			int maxImageHeight = (int) (MAX_IMAGE_SCREEN_PROPORTION_Y * point.x);
			maxImageWidth = maxImageWidth > MAX_IMAGE_DISPLAY_WIDTH ? MAX_IMAGE_DISPLAY_WIDTH : maxImageWidth;
			maxImageHeight = maxImageHeight > MAX_IMAGE_DISPLAY_HEIGHT ? MAX_IMAGE_DISPLAY_HEIGHT : maxImageHeight;
			float scale = ImageUtil.calculateBitmapScaleFactor(options.outWidth, options.outHeight, maxImageWidth, maxImageHeight);
			ret = new Point((int) (scale * options.outWidth), (int) (scale * options.outHeight));
		} catch (Exception e) {
			Log.e("Error opening stored file.", e);
		} finally {
			Util.ensureClosed(fis);
		}
		return ret;
	}

	private void startUpdateUnreadMessageTask(IncomingTextMessage message, int position) {
		UpdateUnreadMessageTask task = new UpdateUnreadMessageTask(position);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, message);
		} else {
			task.execute(message);
		}
	}

	private class UpdateUnreadMessageTask extends AsyncTask<IncomingTextMessage, Void, Void> {
		private int position;

		public UpdateUnreadMessageTask(int position) {
			this.position = position;
		}

		@Override
		protected Void doInBackground(IncomingTextMessage... textMessages) {
			textMessages[0].setRead(true);
			Map<String, String> data = new HashMap<>();
			data.put("message_id", textMessages[0].getId());
			MetricModule.sendMetric(context, Event.EventLabel.message_center__read, null, data);
			MessageManager.updateMessage(context, textMessages[0]);
			MessageManager.notifyHostUnreadMessagesListeners(MessageManager.getUnreadMessageCount(context));
      return null;
		}

		@Override
		protected void onCancelled() {
			positionsWithPendingUpdateTask.remove(new Integer(position));
		}

		@Override
		protected void onPostExecute(Void result) {
			positionsWithPendingUpdateTask.remove(new Integer(position));
		}


	}


	private void startLoadAttachedImageTask(OutgoingFileMessage message, int position, OutgoingFileMessageHolder holder) {
		StoredFile storedFile = message.getStoredFile(context);
		String mimeType = storedFile.getMimeType();
		String imagePath;

		if (mimeType != null) {
			imagePath = storedFile.getLocalFilePath();
			if (mimeType.contains("image")) {
				holder.image.setVisibility(View.INVISIBLE);

				Point dimensions = getBitmapDimensions(storedFile);
				if (dimensions != null) {
					holder.image.setPadding(dimensions.x, dimensions.y, 0, 0);
				}
			}
			LoadAttachedImageTask task = new LoadAttachedImageTask(position, holder);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
				task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, imagePath);
			} else {
				task.execute(imagePath);
			}
		}

	}

	private class LoadAttachedImageTask extends AsyncTask<String, Void, Bitmap> {
		private int position;
		private WeakReference<OutgoingFileMessageHolder> holderRef;

		public LoadAttachedImageTask(int position, OutgoingFileMessageHolder holder) {
			this.position = position;
			this.holderRef = new WeakReference<>(holder);
		}

		@Override
		protected Bitmap doInBackground(String... paths) {
			FileInputStream fis = null;
			Bitmap imageBitmap = null;
			try {
				fis = context.openFileInput(paths[0]);
				Point point = Util.getScreenSize(context);
				int maxImageWidth = (int) (MAX_IMAGE_SCREEN_PROPORTION_X * point.x);
				int maxImageHeight = (int) (MAX_IMAGE_SCREEN_PROPORTION_Y * point.x);
				maxImageWidth = maxImageWidth > MAX_IMAGE_DISPLAY_WIDTH ? MAX_IMAGE_DISPLAY_WIDTH : maxImageWidth;
				maxImageHeight = maxImageHeight > MAX_IMAGE_DISPLAY_HEIGHT ? MAX_IMAGE_DISPLAY_HEIGHT : maxImageHeight;
				// Loading image from File Store. Pass 0 for orientation because images have been rotated when stored
				imageBitmap = ImageUtil.createScaledBitmapFromStream(fis, maxImageWidth, maxImageHeight, null, 0);
				Log.v("Loaded bitmap and re-sized to: %d x %d", imageBitmap.getWidth(), imageBitmap.getHeight());
			} catch (Exception e) {
				Log.e("Error opening stored image.", e);
			} catch (OutOfMemoryError e) {
				// It's generally not a good idea to catch an OutOfMemoryException. But in this case, the OutOfMemoryException
				// had to result from allocating a bitmap, so the system should be in a good state.
				// TODO: Log an event to the server so we know an OutOfMemoryException occurred.
				Log.e("Ran out of memory opening image.", e);
			} finally {
				Util.ensureClosed(fis);
			}
			return imageBitmap;
		}

		@Override
		protected void onCancelled() {
			positionsWithPendingImageTask.remove(new Integer(position));
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			positionsWithPendingImageTask.remove(new Integer(position));
			if (null == bitmap) {
				return;
			}
			OutgoingFileMessageHolder holder = holderRef.get();
			if (holder != null && holder.position == position) {
				if (holder.image != null) {
					holder.image.setPadding(0, 0, 0, 0);
					holder.image.setImageBitmap(bitmap);
					holder.image.setVisibility(View.VISIBLE);
				}
			}
		}
	}


}

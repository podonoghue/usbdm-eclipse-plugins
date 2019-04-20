package net.sourceforge.usbdm.gdb.ttyConsole;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.console.actions.CloseConsoleAction;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * @since 4.12
 */
public class UsbdmTtyConsolePageParticipant implements IConsolePageParticipant {

	private CloseConsoleAction closeAction;

	//   @Override
	//   public <T> T getAdapter(Class<T> adapter) {
	//	   return adapter.cast(null);
	//   }

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return adapter.cast(null);
	}

	@Override
	public void init(IPageBookViewPage page, IConsole console) {
		closeAction = new CloseConsoleAction(console);
		IToolBarManager manager = page.getSite().getActionBars().getToolBarManager();
		manager.appendToGroup(IConsoleConstants.LAUNCH_GROUP, closeAction);  
	}

	@Override
	public void dispose() {
		closeAction = null;
	}

	@Override
	public void activated() {
	}

	@Override
	public void deactivated() {
	}

}

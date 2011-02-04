package wmisample;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.ole.win32.OLE;
import org.eclipse.swt.ole.win32.OleAutomation;
import org.eclipse.swt.ole.win32.Variant;

import wmisample.Helper.VariantVisitor;

public class WMIConnection {
	
	private Variant service;
	private OleAutomation automation;

	public WMIConnection() {
		this.automation = new OleAutomation("WbemScripting.SWbemLocator");
		
		this.service = automation.invoke ( Helper.getId ( automation,"ConnectServer" ) );
		
		if ( service.getType() == OLE.VT_ERROR )
		{
			OLE.error(service.getInt());
			throw new RuntimeException ( String.format ( "Failed to connect to server: %s", service.getString () ) );
		}
	}
	
	public List<WMIObjectInformation> executeQuery ( String query )
	{
		OleAutomation serviceAutomation = service.getAutomation();
		try
		{
			final List<WMIObjectInformation> result = new LinkedList<WMIObjectInformation>();
			
			Variant resultList = serviceAutomation.invoke(Helper.getId(serviceAutomation,"ExecQuery"), new Variant[]{new Variant(query), new Variant("WQL"), new Variant (32)});
			
			if ( resultList == null )
			{
				throw new RuntimeException ( serviceAutomation.getLastError() );
			}
			
			Helper.forEachVariant(resultList, new VariantVisitor() {
				
				@Override
				public void visit(Variant variant) {
					
					final Map<String,Object> params = new HashMap<String, Object>();
					
					Helper.forEachVariant(Helper.getParameter(variant, "Properties_"), new VariantVisitor() {
						
						@Override
						public void visit(Variant variant) {
							params.put(Helper.getParameter ( variant, "Name" ).getString(), Helper.convertVariant ( Helper.getParameter(variant,"Value") ) );
						}
					});
					
					result.add ( new WMIObjectInformation ( Helper.getParameter( Helper.getParameter(variant, "Path_"), "Path").getString(), params ));
				}
			});
			
			return result;
		}
		finally
		{
			serviceAutomation.dispose();
		}
		
	}
	
	public void dispose ()
	{
		this.automation.dispose ();
	}
}

package csiro.pidsvc.mappingstore.condition;

import javax.servlet.http.HttpServletRequest;

import csiro.pidsvc.helper.URI;

public class ConditionQrCodeRequest extends AbstractCondition
{
	public ConditionQrCodeRequest(URI uri, HttpServletRequest request)
	{
		super(uri, request, SpecialConditionType.QR_CODE_REQUEST, null, null);
	}

	@Override
	public boolean matches()
	{
		return false;
	}
}

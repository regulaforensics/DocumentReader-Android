package com.regula.documentreader.demo;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.regula.documentreader.api.enums.eRFID_AccessControl_ProcedureType;
import com.regula.documentreader.api.enums.eRFID_CertificateType;
import com.regula.documentreader.api.enums.eRFID_ErrorCodes;
import com.regula.documentreader.api.results.rfid.AccessControlProcedureType;
import com.regula.documentreader.api.results.rfid.Application;
import com.regula.documentreader.api.results.rfid.File;
import com.regula.documentreader.api.results.rfid.RFIDSessionData;
import com.regula.documentreader.api.results.rfid.SecurityObject;
import com.regula.documentreader.api.translation.TranslationUtil;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class RfidBinaryDataFragment extends Fragment {

    private ImageView bacStatus, paceStatus, caStatus, taStatus, aaStatus, paStatus;
    private LinearLayout datagroupsList;
    private TextView dsIssuerTv, dsSubjectTv, dsValidFromTv, dsValidToTv, dsSignatureTv, dsPkAlgorithmTv;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view =   inflater.inflate(R.layout.rfid_binary_data_fragment,container, false);

        bacStatus = (ImageView) view.findViewById(R.id.bacStatus);
        paceStatus = (ImageView) view.findViewById(R.id.paceStatus);
        caStatus = (ImageView) view.findViewById(R.id.caStatus);
        taStatus = (ImageView) view.findViewById(R.id.taStatus);
        aaStatus = (ImageView) view.findViewById(R.id.aaStatus);
        paStatus = (ImageView) view.findViewById(R.id.paStatus);

        dsIssuerTv = (TextView) view.findViewById(R.id.dsIssuerTv);
        dsSubjectTv = (TextView) view.findViewById(R.id.dsSubjectTv);
        dsValidFromTv = (TextView) view.findViewById(R.id.dsValidFromTv);
        dsValidToTv = (TextView) view.findViewById(R.id.dsValidToTv);
        dsSignatureTv = (TextView) view.findViewById(R.id.dsSignatureTv);
        dsPkAlgorithmTv = (TextView) view.findViewById(R.id.dsPkAlgorithmTv);

        datagroupsList = (LinearLayout) view.findViewById(R.id.datagroupsList);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        RFIDSessionData data = ResultsActivityTabbed.documentReaderResults.rfidSessionData;

        for(AccessControlProcedureType accessControl : data.accessControls){
            int status = accessControl.Status == eRFID_ErrorCodes.RFID_Error_NoError ?  R.drawable.ok :
                    accessControl.Status == eRFID_ErrorCodes.RFID_Error_Failed? R.drawable.fail: R.drawable.undefined;
            switch (accessControl.Type){
                case eRFID_AccessControl_ProcedureType.acptBAC:
                    bacStatus.setImageResource(status);
                    break;
                case eRFID_AccessControl_ProcedureType.acptPACE:
                    paceStatus.setImageResource(status);
                    break;
                case eRFID_AccessControl_ProcedureType.acptCA:
                    caStatus.setImageResource(status);
                    break;
                case eRFID_AccessControl_ProcedureType.acptTA:
                    taStatus.setImageResource(status);
                    break;
                case eRFID_AccessControl_ProcedureType.acptAA:
                    aaStatus.setImageResource(status);
                    break;
            }
        }

        int status = R.drawable.undefined;
        if(data.applications!=null && data.applications.size()>0) {
            Application application = data.applications.get(0);
            for (File file : application.files) {
                if (file.pAStatus == eRFID_ErrorCodes.RFID_Error_NoError) {
                    status = R.drawable.ok;
                } else if (file.pAStatus == eRFID_ErrorCodes.RFID_Error_Failed) {
                    status = R.drawable.fail;
                    break;
                }
            }
            paStatus.setImageResource(status);
        }

        ArrayList<File> items;
        items = new ArrayList<>();
        for(Application app : data.applications){
            for(File file : app.files){
                if(file.fileData!=null){
                    items.add(file);
                }
            }
        }

        datagroupsList.removeAllViews();
        View.inflate(getActivity(), R.layout.rfid_binary_data_item_header_layout, datagroupsList);
        int position=0;
        for(File item: items){
            View v = View.inflate(getActivity(),R.layout.rfid_binary_data_item_layout, null);
            String type = TranslationUtil.getRfidDgTranslation(getActivity(), item.type);
            int dgStatus = item.pAStatus == eRFID_ErrorCodes.RFID_Error_Failed ? R.drawable.fail :
                    item.pAStatus ==eRFID_ErrorCodes.RFID_Error_NoError? R.drawable.ok : R.drawable.undefined;

            TextView dg = (TextView) v.findViewById(R.id.dataGroupTv);
            ImageView dgstatus = (ImageView) v.findViewById(R.id.dataGroupStatusIv);
            TextView length = (TextView) v.findViewById(R.id.dataGroupLengthTv);

            dg.setText(type);
            dgstatus.setImageResource(dgStatus);
            length.setText(String.valueOf(item.fileData.length));

            v.setBackgroundColor(position % 2 > 0 ? Color.rgb(228, 228, 237) : Color.rgb(237, 237, 228));

            datagroupsList.addView(v);
            position++;
        }

        if(data.securityObjects!=null && data.securityObjects.size()>0){
            SecurityObject object = data.securityObjects.get(0);
            if(object.signerInfos!=null && object.signerInfos.size()>0){
                SecurityObject.SignerInfo info = object.signerInfos.get(0);
                if(info.certificateChain!=null){
                    for(SecurityObject.CertificateChain certificateChain:info.certificateChain){
                        if(certificateChain.type == eRFID_CertificateType.ctDS){
                            dsIssuerTv.setText(certificateChain.issuer.friendlyName.data);
                            dsSubjectTv.setText(certificateChain.subject.friendlyName.data);
                            dsSignatureTv.setText(certificateChain.signatureAlgorithm.substring(0, certificateChain.signatureAlgorithm.indexOf('(')));
                            dsPkAlgorithmTv.setText(certificateChain.subjectPKAlgorithm.substring(0, certificateChain.subjectPKAlgorithm.indexOf('(')));

                            try {
                                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(certificateChain.validity.notBefore.format
                                        .replace("YY","yy").replace("DD", "dd"));
                                SimpleDateFormat resultFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss z");
                                DateFormat date = new SimpleDateFormat("z", Locale.getDefault());
                                String localTime = date.format(Calendar.getInstance().getTime());
                                Date from  = simpleDateFormat.parse(certificateChain.validity.notBefore.data.replace("Z",localTime));
                                dsValidFromTv.setText(resultFormat.format(from));
                                Date to  = simpleDateFormat.parse(certificateChain.validity.notAfter.data.replace("Z",localTime));
                                dsValidToTv.setText(resultFormat.format(to));
                            } catch (ParseException e) {
                                e.printStackTrace();
                            }
                            break;
                        }
                    }
                }
            }
        }
    }
}

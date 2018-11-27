/*
 * Copyright (C) 2018 The ontology Authors
 * This file is part of The ontology library.
 *
 *  The ontology is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  The ontology is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with The ontology.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.github.ontio.smartcontract.neovm.abi;

import com.alibaba.fastjson.JSON;
import com.github.ontio.common.ErrorCode;
import com.github.ontio.common.Helper;
import com.github.ontio.core.scripts.ScriptBuilder;
import com.github.ontio.core.scripts.ScriptOp;
import com.github.ontio.sdk.exception.SDKException;

import java.lang.reflect.Array;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class BuildParams {
    public enum Type {
        ByteArrayType(0x00),
        BooleanType(0x01),
        IntegerType(0x02),
        InterfaceType(0x40),
        ArrayType(0x80),
        StructType(0x81),
        MapType(0x82);
        private byte type;

        private Type(int t) {
            this.type = (byte)t;
        }
        public byte getValue(){
            return type;
        }
    }
    /**
     * @param builder
     * @param list
     * @return
     */
    private static byte[] createCodeParamsScript(ScriptBuilder builder, List<Object> list) {
        try {
            for (int i = list.size() - 1; i >= 0; i--) {
                Object val = list.get(i);
                if (val instanceof byte[]) {
                    builder.emitPushByteArray((byte[]) val);
                } else if (val instanceof String) {
                    builder.emitPushByteArray(((String) val).getBytes());
                } else if (val instanceof Boolean) {
                    builder.emitPushBool((Boolean) val);
                } else if(val instanceof Integer){
                    builder.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((int)val)));
                } else if (val instanceof Long) {
                    builder.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((long)val)));
                } else if(val instanceof Map){
                    pushMap(builder,val);
                    //builder.emitPushByteArray(bys);
                } else if(val instanceof Struct){
                    byte[] bys = getStructBytes(val);
                    builder.emitPushByteArray(bys);
                } else if (val instanceof List) {
                    List tmp = (List) val;
                    createCodeParamsScript(builder, tmp);
                    builder.emitPushInteger(new BigInteger(String.valueOf(tmp.size())));
                    builder.pushPack();

                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toArray();
    }
    public static byte[] getStructBytes(Object val){

        ScriptBuilder sb = null;
        try {
            sb = new ScriptBuilder();
            List list = ((Struct)val).list;
            sb.add(Type.StructType.getValue());
            sb.add(Helper.BigIntToNeoBytes(BigInteger.valueOf( list.size())));
            for (int i = 0; i < list.size(); i++) {
                Object eValue = list.get(i);
                if(eValue instanceof byte[]){
                    sb.add(Type.ByteArrayType.getValue());
                    sb.emitPushByteArray((byte[]) eValue);
                } else if(eValue instanceof String){
                    sb.add(Type.ByteArrayType.getValue());
                    sb.emitPushByteArray(((String) eValue).getBytes());
                } else if (eValue instanceof Boolean) {
                    sb.add(Type.BooleanType.getValue());
                    sb.emitPushBool((Boolean) eValue);
                } else if (eValue instanceof Struct) {
                    sb.add(Type.StructType.getValue());
                    sb.emitPushByteArray(getStructBytes(eValue));
                } else if (eValue instanceof List) {
                    List tmp = (List) eValue;
                    createCodeParamsScript(sb, tmp);
                    sb.emitPushInteger(new BigInteger(String.valueOf(tmp.size())));
                    sb.pushPack();
                }else if(eValue instanceof Integer){
                    sb.add(Type.ByteArrayType.getValue());
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Integer)eValue)));
                } else if(eValue instanceof Long){
                    sb.add(Type.ByteArrayType.getValue());
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Long)eValue)));
                } else {
                    throw new SDKException(ErrorCode.ParamError);
                }
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
        return sb.toArray();
    }
    public static void pushParam(ScriptBuilder sb,Object eValue){
        try {
            if (eValue instanceof byte[]) {
                sb.emitPushByteArray((byte[]) eValue);
            } else if (eValue instanceof String) {
                sb.emitPushByteArray(((String) eValue).getBytes());
            } else if (eValue instanceof Boolean) {
                sb.emitPushBool((Boolean) eValue);
            } else if (eValue instanceof Map) {
                pushMap(sb,eValue);
            } else if (eValue instanceof Struct) {
                sb.emitPushByteArray(getStructBytes(eValue));
            } else if (eValue instanceof List) {
                List tmp = (List) eValue;
                createCodeParamsScript(sb, tmp);
                sb.emitPushInteger(new BigInteger(String.valueOf(tmp.size())));
                sb.pushPack();
            } else if (eValue instanceof Integer) {
                sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Integer) eValue)));
            } else if (eValue instanceof Long) {
                sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Long) eValue)));
            } else {
                throw new SDKException(ErrorCode.ParamError);
            }
        }catch (SDKException e) {
            e.printStackTrace();
        }
    }

    public static void pushMap(ScriptBuilder sb,Object val) {
        Map<String, Object> map = (Map) val;
        sb.add(ScriptOp.OP_NEWMAP);
        sb.add(ScriptOp.OP_TOALTSTACK);
        for (Map.Entry e : map.entrySet()) {
            sb.add(ScriptOp.OP_DUPFROMALTSTACK);
            pushParam(sb, e.getKey());
            pushParam(sb, e.getValue());
            sb.add(ScriptOp.OP_SETITEM);
        }
        sb.add(ScriptOp.OP_FROMALTSTACK);
    }
    public static byte[] getMapBytes(Object val){
        ScriptBuilder sb = null;
        try {
            sb = new ScriptBuilder();
            Map<String,Object> map = (Map)val;
            sb.add(Type.MapType.getValue());
            sb.add(Helper.BigIntToNeoBytes(BigInteger.valueOf( map.size())));
            for(Map.Entry e:map.entrySet()){
                sb.add(Type.ByteArrayType.getValue());
                sb.emitPushByteArray(((String) e.getKey()).getBytes());
                Object eValue = e.getValue();
                if(eValue instanceof byte[]){
                    sb.add(Type.ByteArrayType.getValue());
                    sb.emitPushByteArray((byte[]) eValue);
                } else if(eValue instanceof String){
                    sb.add(Type.ByteArrayType.getValue());
                    sb.emitPushByteArray(((String) eValue).getBytes());
                } else if (eValue instanceof Boolean) {
                    sb.add(Type.BooleanType.getValue());
                    sb.emitPushBool((Boolean) eValue);
                } else if (eValue instanceof Map) {
                    sb.add(Type.MapType.getValue());
                    sb.emitPushByteArray(getMapBytes(eValue));
                } else if (eValue instanceof Struct) {
                    sb.add(Type.StructType.getValue());
                    sb.emitPushByteArray(getStructBytes(eValue));
                } else if (eValue instanceof List) {
                    List tmp = (List) eValue;
                    createCodeParamsScript(sb, tmp);
                    sb.emitPushInteger(new BigInteger(String.valueOf(tmp.size())));
                    sb.pushPack();
                }else if(eValue instanceof Integer){
                    sb.add(Type.IntegerType.getValue());
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Integer) eValue)));
                } else if(eValue instanceof Long){
                    sb.add(Type.IntegerType.getValue());
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Long) eValue)));
                } else {
                    throw new SDKException(ErrorCode.ParamError);
                }
            }
        } catch (SDKException e) {
            e.printStackTrace();
        }
        return sb.toArray();
    }
    /**
     * @param list
     * @return
     */
    public static byte[] createCodeParamsScript(List<Object> list) {
        ScriptBuilder sb = new ScriptBuilder();
        try {
            for (int i = list.size() - 1; i >= 0; i--) {
                Object val = list.get(i);
                if (val instanceof byte[]) {
                    sb.emitPushByteArray((byte[]) val);
                } else if (val instanceof String) {
                    sb.emitPushByteArray(((String) val).getBytes());
                } else if (val instanceof Boolean) {
                    sb.emitPushBool((Boolean) val);
                } else if(val instanceof Integer){
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((int)val)));
                } else if (val instanceof Long) {
                    sb.emitPushByteArray(Helper.BigIntToNeoBytes(BigInteger.valueOf((Long) val)));
                } else if(val instanceof BigInteger){
                    sb.emitPushInteger((BigInteger)val);
                } else if(val instanceof Map){
                    pushMap(sb,val);
                    //byte[] bys = getMapBytes(val);
                   // sb.emitPushByteArray(bys);
                } else if(val instanceof Struct){
                    byte[] bys = getStructBytes(val);
                    sb.emitPushByteArray(bys);
                } else if (val instanceof List) {
                    List tmp = (List) val;
                    createCodeParamsScript(sb, tmp);
                    sb.emitPushInteger(new BigInteger(String.valueOf(tmp.size())));
                    sb.pushPack();
                } else {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toArray();
    }

    /**
     * @param abiFunction
     * @return
     * @throws Exception
     */
    public static byte[] serializeAbiFunction( AbiFunction abiFunction) throws Exception {
        List list = new ArrayList<Object>();
        list.add(abiFunction.getName().getBytes());
        List tmp = new ArrayList<Object>();
        for (Parameter obj : abiFunction.getParameters()) {
            if ("ByteArray".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), byte[].class));
            } else if ("String".equals(obj.getType())) {
                tmp.add(obj.getValue());
            } else if ("Boolean".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), boolean.class));
            } else if ("Integer".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Long.class));
            } else if ("Array".equals(obj.getType())) {
                List l = JSON.parseObject(obj.getValue(), List.class);
                l = listConvert(l);
                tmp.add(l);
            } else if ("InteropInterface".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Object.class));
            } else if ("Void".equals(obj.getType())) {

            } else if ("Map".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Map.class));
            } else if ("Struct".equals(obj.getType())) {
                tmp.add(JSON.parseObject(obj.getValue(), Struct.class));
            } else {
                throw new SDKException(ErrorCode.TypeError);
            }
        }
        if(list.size()>0) {
            list.add(tmp);
        }
        byte[] params = createCodeParamsScript(list);
        return params;
    }
    private static List listConvert(List l){
        for(int i = 0; i < l.size();i++){
            if(l.get(i) instanceof String) {
                if (l.get(i) instanceof String) {
                    l.set(i, Base64.getDecoder().decode((String) l.get(i)));
                }
            }else if (l.get(i) instanceof List) {
                l.set(i,listConvert((List)l.get(i)));
            }
        }
        return l;
    }
}